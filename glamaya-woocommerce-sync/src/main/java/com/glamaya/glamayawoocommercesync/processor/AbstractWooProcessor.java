package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.glamayawoocommercesync.monitoring.FetchCycleEvent;
import com.glamaya.glamayawoocommercesync.port.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import com.glamaya.glamayawoocommercesync.service.OAuth1Service;
import com.glamaya.glamayawoocommercesync.util.ModifiedDateResolver;
import com.glamaya.glamayawoocommercesync.woocommerce.WooEntityWithDates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

/**
 * Base Woo processor implementing polling, incremental fetching, adaptive backoff and templated per-entity processing.
 * Subclasses supply: search request construction, entity type, formatting, id extraction and publishing hooks.
 * Common concerns (tracker persistence, OAuth header, date extraction) are handled here.
 * <p>
 * Responsibilities:
 * - Manage polling cadence (active vs passive) & disabled adaptive backoff.
 * - Build and execute WooCommerce queries with OAuth1 signing.
 * - Maintain and persist {@link ProcessorStatusTracker} state (page, lastUpdatedDate, count).
 * - Convert raw response fragments to typed entities and hand them to the generic reactive handle pipeline.
 * - Emit fetch cycle monitoring events.
 */
@Slf4j
public abstract class AbstractWooProcessor<E> implements GlamWoocommerceProcessor<List<E>> {

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;
    protected final PollerMetadata poller;
    protected final long pageSize;
    protected boolean resetOnStartup;
    protected final int activeMillis;
    protected final int passiveMillis;
    protected final String queryUrl;
    protected final boolean enable;
    protected final AtomicInteger consecutiveEmptyPages = new AtomicInteger(0);
    private int disabledPollCount = 0;
    private static final int DISABLED_BASE_DELAY_MS = 1000;
    protected final int processingConcurrency;

    private final ApplicationEventPublisher eventPublisher;
    private final ConcurrentLinkedQueue<List<E>> resultsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean fetchInProgress = new AtomicBoolean(false);

    protected final StatusTrackerStore statusTrackerStore; // new
    protected final OAuth1Service oAuth1Service; // new

    /**
     * Constructor wiring core collaborators required for polling & processing.
     */
    protected AbstractWooProcessor(WebClient webClient,
                                   ObjectMapper objectMapper,
                                   PollerMetadata poller,
                                   long pageSize,
                                   boolean resetOnStartup,
                                   int activeMillis,
                                   int passiveMillis,
                                   String queryUrl,
                                   boolean enable,
                                   int processingConcurrency,
                                   OAuth1Service oAuth1Service,
                                   StatusTrackerStore statusTrackerStore,
                                   ApplicationEventPublisher eventPublisher) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.poller = poller;
        this.pageSize = pageSize;
        this.resetOnStartup = resetOnStartup;
        this.activeMillis = activeMillis;
        this.passiveMillis = passiveMillis;
        this.queryUrl = queryUrl;
        this.enable = enable;
        this.processingConcurrency = processingConcurrency;
        this.oAuth1Service = oAuth1Service;
        this.statusTrackerStore = statusTrackerStore;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @return The logical processor type used for tracker lookup and monitoring events.
     */
    public abstract ProcessorType getProcessorType();

    /**
     * Build search request POJO representing query parameter state for current page / lastUpdatedDate.
     *
     * @param tracker current persisted tracker.
     * @return search request object convertible to a Map via ObjectMapper.
     */
    protected abstract Object buildSearchRequest(ProcessorStatusTracker tracker);

    /**
     * Extract modified date string from an entity. Uses marker interface first then reflective fallbacks.
     *
     * @return function mapping arbitrary entity (Object) to a best-effort modified date string or null.
     */
    public Function<Object, String> dateExtractorFunction() {
        return o -> {
            try {
                if (o instanceof WooEntityWithDates w) {
                    return ModifiedDateResolver.resolve(w.getDateModifiedGmt(), w.getDateModified(), w.getDateCreated());
                }
                // Reflection fallback
                Method mGmt = safeMethod(o, "getDateModifiedGmt");
                Method mMod = safeMethod(o, "getDateModified");
                Method mCre = safeMethod(o, "getDateCreated");
                String gmt = invokeString(o, mGmt);
                String mod = invokeString(o, mMod);
                String cre = invokeString(o, mCre);
                if (gmt != null || mod != null || cre != null) {
                    return ModifiedDateResolver.resolve(gmt, mod, cre);
                }
            } catch (Exception ignored) {
            }
            return null;
        };
    }

    private Method safeMethod(Object o, String name) {
        try {
            return o.getClass().getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private String invokeString(Object o, Method m) {
        try {
            return m == null ? null : (String) m.invoke(o);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Persist tracker asynchronously.
     *
     * @param tracker tracker instance with updated state.
     * @return mono completing with persisted tracker.
     */
    protected Mono<ProcessorStatusTracker> saveTracker(ProcessorStatusTracker tracker) {
        return statusTrackerStore.save(tracker);
    }

    /**
     * Retrieve or initialize tracker for this processor.
     *
     * @param resetOnStartup whether reset is requested for first poll.
     * @param pageSize       configured page size.
     * @return mono of existing or new tracker.
     */
    protected Mono<ProcessorStatusTracker> getOrCreateTracker(boolean resetOnStartup, long pageSize) {
        return statusTrackerStore.getOrCreate(getProcessorType(), resetOnStartup, pageSize);
    }

    /**
     * Build OAuth1 header value for the request.
     *
     * @param queryUrl    endpoint path.
     * @param queryParams already-filtered (non-null/non-blank) query params.
     * @return authorization header string.
     */
    protected String getOAuthHeader(String queryUrl, Map<String, String> queryParams) {
        return oAuth1Service.generateOAuth1Header(queryUrl, queryParams);
    }

    /**
     * @return concrete entity class used for JSON -> POJO conversion.
     */
    protected abstract Class<E> getEntityClass();

    /**
     * Optional entity formatting/transformation hook. Defaults to identity.
     *
     * @param entity raw entity.
     * @return formatted entity.
     */
    protected E doFormat(E entity) {
        return entity;
    }

    /**
     * Extract stable identifier used for logging and Kafka key selection.
     *
     * @param entity entity instance.
     * @return identifier object (String/Long/etc).
     */
    protected abstract Object getEntityId(E entity);

    /**
     * Publish the primary domain event for the formatted entity.
     *
     * @param formatted entity after formatting.
     */
    protected abstract void publishPrimaryEvent(E formatted);

    /**
     * Publish optional secondary derived event (e.g., Contact). No-op by default.
     *
     * @param formatted entity.
     */
    protected void publishSecondaryEvent(E formatted) { /* optional */ }

    /**
     * Notification hook executed after successful processing of an entity.
     *
     * @param formatted entity.
     * @param ctx       context map (id, entity type, etc.).
     */
    protected void notifySuccess(E formatted, Map<String, Object> ctx) { /* optional */ }

    /**
     * Notification hook executed on processing error.
     *
     * @param original original entity.
     * @param e        thrown exception.
     * @param ctx      context map (id, entity type, error message).
     */
    protected void notifyError(E original, Exception e, Map<String, Object> ctx) { /* optional */ }

    /**
     * Exposes a message source adapting internal polling logic to Spring Integration.
     *
     * @return message source delivering batches of entities when available.
     */
    @Override
    public MessageSource<List<E>> receive() {
        return this::pollOnce;
    }

    /**
     * Single poll attempt invoked by the framework. Handles disabled backoff, batch draining and async fetch kickoff.
     *
     * @return Message with payload list or null if nothing ready.
     */
    private Message<List<E>> pollOnce() {
        if (!enable) {
            handleDisabled();
            return null;
        }
        disabledPollCount = 0; // reset disabled counter if enabled

        List<E> ready = drainReadyBatch();
        if (ready != null) {
            return MessageBuilder.withPayload(ready).build();
        }

        startAsyncFetchIfNeeded();
        return null; // no batch available yet
    }

    /**
     * Apply exponential + jitter backoff when processor is disabled, adjusting poller interval.
     */
    private void handleDisabled() {
        disabledPollCount++;
        long exponential = (long) (DISABLED_BASE_DELAY_MS * Math.pow(2, Math.min(disabledPollCount, 8)));
        long capped = Math.min(exponential, passiveMillis * 10L);
        long jitter = java.util.concurrent.ThreadLocalRandom.current().nextLong(0, DISABLED_BASE_DELAY_MS);
        long nextDelay = capped + jitter;
        if (disabledPollCount % 10 == 1) {
            log.info("{} disabled; backoff={}ms (attempt={})", getProcessorType(), nextDelay, disabledPollCount);
        }
        modifyPollerDuration(poller, (int) nextDelay);
    }

    /**
     * Drain one completed batch from the internal queue if present.
     *
     * @return list of entities or null.
     */
    private List<E> drainReadyBatch() {
        List<E> ready = resultsQueue.poll();
        if (ready != null && !ready.isEmpty()) {
            log.debug("{} delivering batch size={}", getProcessorType(), ready.size());
            return ready;
        }
        return null;
    }

    /**
     * Initiate asynchronous fetch sequence if not already in progress.
     */
    private void startAsyncFetchIfNeeded() {
        if (!fetchInProgress.compareAndSet(false, true)) {
            return; // already fetching
        }
        long startNanos = System.nanoTime();
        getOrCreateTracker(resetOnStartup, pageSize)
                .doOnNext(t -> resetOnStartup = false)
                .flatMap(tracker -> executeFetch(tracker, startNanos))
                .subscribe(list -> {
                    if (!list.isEmpty()) {
                        resultsQueue.offer(list);
                    }
                    fetchInProgress.set(false);
                }, err -> fetchInProgress.set(false));
    }

    /**
     * Execute the remote fetch, update tracker, publish monitoring event and return list of entities.
     *
     * @param tracker    current tracker state.
     * @param startNanos start time for latency measurement.
     * @return mono with fetched list (possibly empty).
     */
    private Mono<List<E>> executeFetch(ProcessorStatusTracker tracker, long startNanos) {
        Object searchRequest = buildSearchRequest(tracker);
        Map<String, String> queryParams = buildFilteredQueryParams(searchRequest);
        String oauthHeader = getOAuthHeader(queryUrl, queryParams);
        log.info("{} fetch started with params={}", getProcessorType(), queryParams);

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(queryUrl);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, oauthHeader == null ? "" : oauthHeader)
                .retrieve()
                .bodyToFlux(Object.class)
                .map(this::convertFragmentSafely)
                .filter(Objects::nonNull)
                .collectList()
                .onErrorResume(e -> {
                    long elapsed = System.nanoTime() - startNanos;
                    log.error("{} fetch error: {}", getProcessorType(), e.toString());
                    eventPublisher.publishEvent(new FetchCycleEvent(getProcessorType(), 0, true, true, elapsed));
                    return Mono.just(List.of());
                })
                .flatMap(list -> processFetchResult(tracker, list, startNanos));
    }

    /**
     * Convert a raw response fragment to the target entity type using ObjectMapper, logging failures.
     *
     * @param fragment raw fragment.
     * @return typed entity or null if conversion fails.
     */
    private E convertFragmentSafely(Object fragment) {
        if (fragment == null) return null;
        if (getEntityClass().isInstance(fragment)) return (E) fragment;
        try {
            return objectMapper.convertValue(fragment, getEntityClass());
        } catch (IllegalArgumentException ex) {
            log.error("Conversion failure {} fragment={}", getProcessorType(), fragment);
            return null;
        }
    }

    /**
     * Handle result list: adaptive backoff on empty, tracker update on non-empty, monitoring event, persistence.
     *
     * @param tracker    status tracker.
     * @param list       fetched entities.
     * @param startNanos start time.
     * @return mono with same list after tracker persistence.
     */
    private Mono<List<E>> processFetchResult(ProcessorStatusTracker tracker, List<E> list, long startNanos) {
        long elapsed = System.nanoTime() - startNanos;
        boolean empty = list.isEmpty();
        if (empty) {
            int emptyCount = consecutiveEmptyPages.incrementAndGet();
            long backoff = Math.min((long) (activeMillis * Math.pow(2, emptyCount)), passiveMillis);
            log.info("No new {} (emptyCount={}), backoff={}ms", getProcessorType(), emptyCount, backoff);
            resetStatusTracker(tracker);
            modifyPollerDuration(poller, (int) backoff);
        } else {
            consecutiveEmptyPages.set(0);
            updateStatusTracker(tracker, list, dateExtractorFunction());
            modifyPollerDuration(poller, activeMillis);
        }
        eventPublisher.publishEvent(new FetchCycleEvent(getProcessorType(), list.size(), empty, false, elapsed));
        return saveTracker(tracker).subscribeOn(Schedulers.boundedElastic()).thenReturn(list);
    }

    /**
     * Convert search request object -> filtered query string map (remove null/blank values).
     *
     * @param searchRequest built POJO.
     * @return map of query params.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> buildFilteredQueryParams(Object searchRequest) {
        Map<String, Object> rawMap = objectMapper.convertValue(searchRequest, Map.class);
        return rawMap == null ? Map.of() : rawMap.entrySet().stream()
                .filter(e -> e.getValue() != null && !String.valueOf(e.getValue()).isBlank())
                .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Generic reactive handle implementation processing each entity concurrently using {@link #processingConcurrency}.
     *
     * @param payload batch of entities.
     * @param headers message headers (unused currently).
     * @return null (fire-and-forget processing).
     */
    @Override
    public Object handle(List<E> payload, MessageHeaders headers) {
        if (payload == null || payload.isEmpty()) return null;
        Flux.fromIterable(payload)
                .flatMap(e -> Mono.fromRunnable(() -> processEntityInternal(e)), processingConcurrency)
                .subscribe();
        return null;
    }

    /**
     * Internal per-entity processing chain: format, publish events, notifications, error handling.
     *
     * @param entity raw entity.
     */
    private void processEntityInternal(E entity) {
        Object idForLog = null;
        try {
            E formatted = doFormat(entity);
            idForLog = getEntityId(formatted);
            Map<String, Object> ctx = new java.util.HashMap<>();
            ctx.put("entity", getProcessorType().name().toLowerCase());
            ctx.put("id", idForLog);
            publishPrimaryEvent(formatted);
            publishSecondaryEvent(formatted);
            notifySuccess(formatted, ctx);
        } catch (Exception ex) {
            Map<String, Object> ctx = new java.util.HashMap<>();
            if (idForLog == null) {
                try {
                    idForLog = getEntityId(entity);
                } catch (Exception ignored) {
                }
            }
            ctx.put("entity", getProcessorType().name().toLowerCase());
            ctx.put("id", idForLog);
            ctx.put("error", ex.getMessage());
            log.error("{} processing error id={}", getProcessorType(), idForLog, ex);
            notifyError(entity, ex, ctx);
        }
    }

    /**
     * Reset tracker to first page and enable lastUpdatedDate usage after an empty response.
     *
     * @param statusTracker tracker.
     */
    private void resetStatusTracker(ProcessorStatusTracker statusTracker) {
        statusTracker.setPage(1);
        statusTracker.setUseLastUpdatedDateInQuery(true);
    }

    /**
     * Advance tracker state given non-empty response list.
     *
     * @param statusTracker   tracker.
     * @param response        entities.
     * @param getDateModified date extractor function.
     */
    private void updateStatusTracker(ProcessorStatusTracker statusTracker, List<?> response,
                                     Function<Object, String> getDateModified) {
        if (response == null || response.isEmpty()) {
            return;
        }
        long newCount = statusTracker.getCount() + response.size();
        statusTracker.setCount(newCount);
        statusTracker.setPage(statusTracker.getPage() + 1);
        String lastDateModified = getDateModified.apply(response.getLast());
        if (lastDateModified != null) {
            statusTracker.setLastUpdatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(lastDateModified));
        }
    }

    /**
     * @return consumer configuring Spring Integration poller using this instance's {@link #getPoller()}.
     */
    public Consumer<SourcePollingChannelAdapterSpec> poll() {
        return e -> e.poller(getPoller());
    }

    /**
     * Dynamically update poller trigger duration (used for adaptive backoff).
     *
     * @param pollerMetadata poller metadata.
     * @param millis         new duration in milliseconds.
     */
    private void modifyPollerDuration(PollerMetadata pollerMetadata, int millis) {
        DynamicPeriodicTrigger trigger = (DynamicPeriodicTrigger) pollerMetadata.getTrigger();
        trigger.setDuration(Duration.ofMillis(millis));
    }

    /**
     * @return underlying poller metadata (exposed for Integration DSL wiring).
     */
    @Override
    public PollerMetadata getPoller() {
        return this.poller;
    }
}

