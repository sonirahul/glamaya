package com.glamaya.glamayawoocommercesync.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.glamayawoocommercesync.config.ApplicationProperties;
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatus;
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatusService;
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import com.glamaya.glamayawoocommercesync.monitoring.FetchCycleEvent;
import com.glamaya.glamayawoocommercesync.port.out.OAuthSignerPort;
import com.glamaya.glamayawoocommercesync.port.out.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.port.out.WooCommerceApiClientPort;
import com.glamaya.glamayawoocommercesync.util.ModifiedDateResolver;
import com.glamaya.glamayawoocommercesync.woocommerce.WooEntityWithDates;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractWooProcessor<E> implements GlamWoocommerceProcessor<List<E>> {

    // Configuration fields
    protected final ObjectMapper objectMapper;
    protected final PollerMetadata poller;
    protected final long pageSize;
    protected boolean resetOnStartup;
    protected final int activeMillis;
    protected final int passiveMillis;
    protected final String queryUrl;
    protected final boolean enable;
    protected final int processingConcurrency;

    // Infrastructure Ports and Services
    private final ApplicationEventPublisher eventPublisher;
    protected final StatusTrackerStore statusTrackerStore;
    protected final OAuthSignerPort oAuth1Service;
    private final ProcessorStatusService processorStatusService;
    private final WooCommerceApiClientPort wooCommerceApiClient;

    // State Management
    private final ConcurrentLinkedQueue<List<E>> resultsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean fetchInProgress = new AtomicBoolean(false);
    private final AtomicInteger consecutiveEmptyPages = new AtomicInteger(0);
    private int disabledPollCount = 0;
    private int recentFailures = 0;
    private long circuitOpenedAt = -1L;

    // Monitoring
    private final ApplicationProperties applicationProperties;
    private final MeterRegistry meterRegistry;
    private final Timer fetchTimer;
    private final io.micrometer.core.instrument.Counter retryCounter;

    private static final int DISABLED_BASE_DELAY_MS = 1000;
    private static final int DEFAULT_FETCH_TIMEOUT_MS = 30000;

    protected AbstractWooProcessor(ObjectMapper objectMapper,
                                   PollerMetadata poller,
                                   long pageSize,
                                   boolean resetOnStartup,
                                   int activeMillis,
                                   int passiveMillis,
                                   String queryUrl,
                                   boolean enable,
                                   int processingConcurrency,
                                   OAuthSignerPort oAuth1Service,
                                   StatusTrackerStore statusTrackerStore,
                                   ProcessorStatusService processorStatusService,
                                   WooCommerceApiClientPort wooCommerceApiClient,
                                   ApplicationEventPublisher eventPublisher,
                                   ApplicationProperties applicationProperties,
                                   MeterRegistry meterRegistry) {
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
        this.processorStatusService = processorStatusService;
        this.wooCommerceApiClient = wooCommerceApiClient;
        this.eventPublisher = eventPublisher;
        this.applicationProperties = applicationProperties;
        this.meterRegistry = meterRegistry;
        this.fetchTimer = meterRegistry != null ? meterRegistry.timer("woocommerce.fetch.timer", "processor", getProcessorType().name()) : null;
        this.retryCounter = meterRegistry != null ? meterRegistry.counter("woocommerce.fetch.retries", "processor", getProcessorType().name()) : null;
    }

    // --- Abstract Methods for Subclasses ---

    public abstract ProcessorType getProcessorType();
    protected abstract Object buildSearchRequest(ProcessorStatus tracker);
    protected abstract Class<E> getEntityClass();
    protected abstract Object getEntityId(E entity);
    protected abstract void publishPrimaryEvent(E formatted);

    // --- Core Orchestration ---

    @Override
    public MessageSource<List<E>> receive() {
        return this::processNextBatch;
    }

    private Message<List<E>> processNextBatch() {
        if (!isProcessorEnabled()) {
            return null;
        }
        List<E> readyBatch = drainReadyBatch();
        if (readyBatch != null) {
            return MessageBuilder.withPayload(readyBatch).build();
        }
        initiateFetchIfNotRunning();
        return null;
    }

    private void initiateFetchIfNotRunning() {
        if (!fetchInProgress.compareAndSet(false, true)) {
            return;
        }
        if (isCircuitBreakerOpen()) {
            log.warn("Circuit breaker is open for processor={}. Skipping fetch.", getProcessorType());
            fetchInProgress.set(false);
            return;
        }
        long startNanos = System.nanoTime();
        getOrCreateTracker(this.resetOnStartup, this.pageSize)
                .doOnNext(t -> this.resetOnStartup = false)
                .flatMap(tracker -> fetchAndProcess(tracker, startNanos))
                .doFinally(sig -> fetchInProgress.set(false))
                .subscribe(
                        processedList -> {
                            if (!processedList.isEmpty()) {
                                resultsQueue.offer(processedList);
                            }
                        },
                        error -> log.error("Unhandled error in the main reactive chain for processor={}", getProcessorType(), error)
                );
    }

    private Mono<List<E>> fetchAndProcess(ProcessorStatus tracker, long startNanos) {
        return fetchDataFromWooCommerce(tracker)
                .map(this::deserializeEntity)
                .filter(Objects::nonNull)
                .collectList()
                .timeout(Duration.ofMillis(DEFAULT_FETCH_TIMEOUT_MS))
                .retryWhen(configureRetryPolicy())
                .flatMap(entities -> handleFetchResult(tracker, entities, startNanos))
                .onErrorResume(error -> handleFetchError(error, startNanos));
    }

    // --- Helper Methods for the Reactive Chain ---

    private Flux<Object> fetchDataFromWooCommerce(ProcessorStatus tracker) {
        Object searchRequest = buildSearchRequest(tracker);
        Map<String, String> queryParams = buildFilteredQueryParams(searchRequest);
        String oauthHeader = oAuth1Service.generateOAuth1Header(queryUrl, queryParams);
        log.info("Starting fetch for processor={} with params={}", getProcessorType(), queryParams);
        return wooCommerceApiClient.fetch(queryUrl, queryParams, oauthHeader);
    }

    private E deserializeEntity(Object fragment) {
        if (fragment == null) return null;
        if (getEntityClass().isInstance(fragment)) {
            return getEntityClass().cast(fragment);
        }
        try {
            return objectMapper.convertValue(fragment, getEntityClass());
        } catch (IllegalArgumentException ex) {
            log.error("Failed to convert fragment to entity for processor={}. Fragment: {}", getProcessorType(), fragment, ex);
            return null;
        }
    }

    private Retry configureRetryPolicy() {
        ApplicationProperties.RetryConfig retryConfig = applicationProperties.getProcessing().retry();
        return Retry.backoff(retryConfig.maxAttempts(), Duration.ofMillis(retryConfig.initialDelayMs()))
                .filter(this::isTransientError)
                .maxBackoff(Duration.ofMillis(retryConfig.maxBackoffMs()))
                .doBeforeRetry(retrySignal -> {
                    if (retryCounter != null) retryCounter.increment();
                    log.warn("Retrying fetch for processor={} due to transient error. Attempt: {}", getProcessorType(), retrySignal.totalRetries() + 1);
                });
    }

    private Mono<List<E>> handleFetchResult(ProcessorStatus tracker, List<E> entities, long startNanos) {
        if (entities.isEmpty()) {
            handleEmptyFetchResult(tracker);
        } else {
            handleSuccessfulFetchResult(tracker, entities);
        }
        recordMetrics(entities.size(), false, startNanos);
        return saveTracker(tracker).subscribeOn(Schedulers.boundedElastic()).thenReturn(entities);
    }

    private void handleEmptyFetchResult(ProcessorStatus tracker) {
        int emptyCount = consecutiveEmptyPages.incrementAndGet();
        long backoff = Math.min((long) (activeMillis * Math.pow(2, emptyCount)), passiveMillis);
        log.info("No new entities found for processor={}. Empty pages in a row: {}. Backing off for {}ms.", getProcessorType(), emptyCount, backoff);
        processorStatusService.resetAfterEmptyPage(tracker);
        modifyPollerDuration(poller, (int) backoff);
    }

    private void handleSuccessfulFetchResult(ProcessorStatus tracker, List<E> entities) {
        consecutiveEmptyPages.set(0);
        recentFailures = 0;
        processorStatusService.advanceAfterBatch(tracker, entities, dateExtractorFunction());
        modifyPollerDuration(poller, activeMillis);
    }

    private Mono<List<E>> handleFetchError(Throwable error, long startNanos) {
        log.error("Error during fetch for processor={}: {}", getProcessorType(), error.getMessage(), error);
        recentFailures++;
        if (applicationProperties.getProcessing().retry().enableCircuitBreaker() && recentFailures >= applicationProperties.getProcessing().retry().circuitBreakerFailureThreshold()) {
            circuitOpenedAt = System.currentTimeMillis();
            log.warn("Circuit breaker opened for processor={}", getProcessorType());
        }
        recordMetrics(0, true, startNanos);
        return Mono.just(List.of());
    }

    private void recordMetrics(int itemCount, boolean isError, long startNanos) {
        long elapsedNanos = System.nanoTime() - startNanos;
        if (fetchTimer != null) {
            fetchTimer.record(elapsedNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
        eventPublisher.publishEvent(new FetchCycleEvent(getProcessorType(), itemCount, itemCount == 0 && !isError, isError, elapsedNanos));
    }

    // --- Entity Processing ---

    @Override
    public Object handle(List<E> payload, MessageHeaders headers) {
        if (payload == null || payload.isEmpty()) return null;
        Flux.fromIterable(payload)
                .limitRate(applicationProperties.getProcessing().bulkhead().limitRate())
                .flatMap(this::processSingleEntity, processingConcurrency)
                .subscribe();
        return null;
    }

    private Mono<Void> processSingleEntity(E entity) {
        return Mono.fromRunnable(() -> {
            Object idForLog = null;
            try {
                E formatted = doFormat(entity);
                idForLog = getEntityId(formatted);
                Map<String, Object> ctx = Map.of("entity", getProcessorType().name().toLowerCase(), "id", idForLog);
                publishPrimaryEvent(formatted);
                publishSecondaryEvent(formatted);
                notifySuccess(formatted, ctx);
            } catch (Exception ex) {
                if (idForLog == null) {
                    try {
                        idForLog = getEntityId(entity);
                    } catch (Exception ignored) {
                    }
                }
                Map<String, Object> errorCtx = Map.of("entity", getProcessorType().name().toLowerCase(), "id", idForLog, "error", ex.getMessage());
                log.error("Error processing entity for processor={} with id={}: {}", getProcessorType(), idForLog, ex.getMessage(), ex);
                notifyError(entity, ex, errorCtx);
            }
        });
    }

    // --- Re-added Delegate Methods ---

    protected Mono<ProcessorStatus> getOrCreateTracker(boolean resetOnStartup, long pageSize) {
        return statusTrackerStore.getOrCreate(getProcessorType(), resetOnStartup, pageSize);
    }

    protected Mono<ProcessorStatus> saveTracker(ProcessorStatus tracker) {
        return statusTrackerStore.save(tracker);
    }

    public Function<Object, String> dateExtractorFunction() {
        return o -> {
            try {
                if (o instanceof WooEntityWithDates w) {
                    return ModifiedDateResolver.resolve(w.getDateModifiedGmt(), w.getDateModified(), w.getDateCreated());
                }
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

    // --- Optional Hooks for Subclasses ---

    protected E doFormat(E entity) {
        return entity;
    }

    protected void publishSecondaryEvent(E formatted) {
    }

    protected void notifySuccess(E formatted, Map<String, Object> ctx) {
    }

    protected void notifyError(E original, Exception e, Map<String, Object> ctx) {
    }

    // --- Utility and Helper Methods ---

    private boolean isProcessorEnabled() {
        if (!enable) {
            disabledPollCount++;
            long exponential = (long) (DISABLED_BASE_DELAY_MS * Math.pow(2, Math.min(disabledPollCount, 8)));
            long capped = Math.min(exponential, passiveMillis * 10L);
            long jitter = ThreadLocalRandom.current().nextLong(0, DISABLED_BASE_DELAY_MS);
            long nextDelay = capped + jitter;
            if (disabledPollCount % 10 == 1) {
                log.info("{} processor is disabled. Backing off for {}ms.", getProcessorType(), nextDelay);
            }
            modifyPollerDuration(poller, (int) nextDelay);
            return false;
        }
        disabledPollCount = 0;
        return true;
    }

    private List<E> drainReadyBatch() {
        List<E> ready = resultsQueue.poll();
        if (ready != null && !ready.isEmpty()) {
            log.debug("Delivering batch of {} entities for processor={}", ready.size(), getProcessorType());
            return ready;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> buildFilteredQueryParams(Object searchRequest) {
        Map<String, Object> rawMap = objectMapper.convertValue(searchRequest, Map.class);
        return rawMap == null ? Map.of() : rawMap.entrySet().stream()
                .filter(e -> e.getValue() != null && !String.valueOf(e.getValue()).isBlank())
                .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void modifyPollerDuration(PollerMetadata pollerMetadata, int millis) {
        if (pollerMetadata.getTrigger() instanceof DynamicPeriodicTrigger trigger) {
            trigger.setDuration(Duration.ofMillis(millis));
        }
    }

    private boolean isTransientError(Throwable t) {
        return t instanceof TimeoutException || t instanceof IOException;
    }

    private boolean isCircuitBreakerOpen() {
        if (!applicationProperties.getProcessing().retry().enableCircuitBreaker() || circuitOpenedAt == -1L) {
            return false;
        }
        long resetMs = applicationProperties.getProcessing().retry().circuitBreakerResetMs();
        if (System.currentTimeMillis() - circuitOpenedAt > resetMs) {
            log.info("Circuit breaker is now closed for processor={}", getProcessorType());
            circuitOpenedAt = -1L;
            recentFailures = 0;
            return false;
        }
        return true;
    }

    @Override
    public PollerMetadata getPoller() {
        return this.poller;
    }

    public Consumer<SourcePollingChannelAdapterSpec> poll() {
        return e -> e.poller(getPoller());
    }
}
