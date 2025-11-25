package com.glamaya.glamayawoocommercesync.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.glamayawoocommercesync.adapter.in.WooCommercePollingAdapter;
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
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract base class for application services that process WooCommerce entities.
 * This class encapsulates the core logic for fetching data, handling resilience (retries, circuit breaker),
 * processing entities, and managing the processor's status. It is designed to be framework-agnostic
 * in its core operations, relying on ports for external interactions.
 *
 * @param <E> The type of the WooCommerce entity this service handles.
 */
@Slf4j
public abstract class AbstractApplicationService<E> implements org.springframework.integration.core.GenericHandler<List<E>> {

    // Core Dependencies
    protected final ObjectMapper objectMapper;
    protected final long pageSize;
    protected boolean resetOnStartup;
    protected final String queryUrl;
    protected final boolean enable; // Indicates if the processor is globally enabled
    protected final int processingConcurrency;

    // Ports and Domain Services
    private final ApplicationEventPublisher eventPublisher;
    protected final StatusTrackerStore statusTrackerStore;
    protected final OAuthSignerPort oAuth1Service;
    private final ProcessorStatusService processorStatusService;
    private final WooCommerceApiClientPort wooCommerceApiClient;

    // Resilience State
    private int recentFailures = 0;
    private long circuitOpenedAt = -1L;

    // Monitoring Dependencies
    private final ApplicationProperties applicationProperties;
    private final Timer fetchTimer;
    private final io.micrometer.core.instrument.Counter retryCounter;

    private static final int DEFAULT_FETCH_TIMEOUT_MS = 30000;

    /**
     * Constructs a new {@code AbstractApplicationService}.
     *
     * @param objectMapper           The {@link ObjectMapper} for JSON serialization/deserialization.
     * @param pageSize               The number of entities to fetch per page.
     * @param resetOnStartup         Whether to reset the processor's status on startup.
     * @param queryUrl               The base URL for the WooCommerce API endpoint.
     * @param enable                 Whether this processor is enabled.
     * @param processingConcurrency  The concurrency level for processing individual entities.
     * @param oAuth1Service          The {@link OAuthSignerPort} for OAuth1 signature generation.
     * @param statusTrackerStore     The {@link StatusTrackerStore} for managing processor status.
     * @param processorStatusService The {@link ProcessorStatusService} for domain-specific status logic.
     * @param wooCommerceApiClient   The {@link WooCommerceApiClientPort} for making API calls.
     * @param eventPublisher         The {@link ApplicationEventPublisher} for publishing monitoring events.
     * @param applicationProperties  The application's configuration properties.
     * @param meterRegistry          The {@link MeterRegistry} for metrics.
     */
    protected AbstractApplicationService(ObjectMapper objectMapper,
                                         long pageSize,
                                         boolean resetOnStartup,
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
        this.pageSize = pageSize;
        this.resetOnStartup = resetOnStartup;
        this.queryUrl = queryUrl;
        this.enable = enable;
        this.processingConcurrency = processingConcurrency;
        this.oAuth1Service = oAuth1Service;
        this.statusTrackerStore = statusTrackerStore;
        this.processorStatusService = processorStatusService;
        this.wooCommerceApiClient = wooCommerceApiClient;
        this.eventPublisher = eventPublisher;
        this.applicationProperties = applicationProperties;
        this.fetchTimer = meterRegistry != null ? meterRegistry.timer("woocommerce.fetch.timer", "processor", getProcessorType().name()) : null;
        this.retryCounter = meterRegistry != null ? meterRegistry.counter("woocommerce.fetch.retries", "processor", getProcessorType().name()) : null;
    }

    // --- Abstract Methods for Subclasses ---

    /**
     * Gets the unique type of the processor.
     *
     * @return The {@link ProcessorType}.
     */
    public abstract ProcessorType getProcessorType();

    /**
     * Builds the specific search request object for the WooCommerce API call.
     *
     * @param tracker The current status tracker containing pagination and date information.
     * @return A search request object.
     */
    protected abstract Object buildSearchRequest(ProcessorStatus tracker);

    /**
     * Gets the class of the entity being processed.
     *
     * @return The entity's {@link Class}.
     */
    protected abstract Class<E> getEntityClass();

    /**
     * Extracts a stable identifier from the entity, used for logging and Kafka keys.
     *
     * @param entity The entity instance.
     * @return A stable identifier (e.g., String, Long).
     */
    protected abstract Object getEntityId(E entity);

    /**
     * Publishes the primary domain event for the processed entity.
     *
     * @param formatted The entity after any formatting.
     */
    protected abstract void publishPrimaryEvent(E formatted);

    // --- Core Fetch and Process Logic ---

    /**
     * Executes the core fetch and process logic for a batch of entities.
     * This method is called by the {@link WooCommercePollingAdapter} (driving adapter).
     *
     * @return A {@link Mono<Boolean>} indicating if the fetch resulted in an empty list ({@code true} for empty, {@code false} for non-empty or error).
     */
    public Mono<Boolean> fetchAndProcess() {
        if (!enable) {
            log.debug("Processor {} is disabled. Skipping fetch.", getProcessorType());
            return Mono.just(true); // Treat as empty to allow backoff
        }
        if (isCircuitBreakerOpen()) {
            log.warn("Circuit breaker is open for processor={}. Skipping fetch.", getProcessorType());
            return Mono.just(true); // Treat as empty to allow backoff
        }

        long startNanos = System.nanoTime();
        return getOrCreateTracker(this.resetOnStartup, this.pageSize)
                .doOnNext(t -> this.resetOnStartup = false) // Reset only once after initial tracker retrieval
                .flatMap(tracker -> executeFetch(tracker, startNanos))
                .map(List::isEmpty) // Return true if list is empty
                .onErrorResume(error -> handleFetchError(error, startNanos).thenReturn(true)); // Treat error as empty for backoff
    }

    /**
     * Executes the actual data fetching from the WooCommerce API and subsequent processing.
     *
     * @param tracker    The current {@link ProcessorStatus} for this processor.
     * @param startNanos The start time in nanoseconds for metrics tracking.
     * @return A {@link Mono} emitting a list of processed entities.
     */
    private Mono<List<E>> executeFetch(ProcessorStatus tracker, long startNanos) {
        Object searchRequest = buildSearchRequest(tracker);
        Map<String, String> queryParams = buildFilteredQueryParams(searchRequest);
        String oauthHeader = oAuth1Service.generateOAuth1Header(queryUrl, queryParams);
        log.info("Starting fetch for processor={} with params={}", getProcessorType(), queryParams);

        return wooCommerceApiClient.fetch(queryUrl, queryParams, oauthHeader)
                .map(this::deserializeEntity)
                .filter(Objects::nonNull)
                .collectList()
                .timeout(Duration.ofMillis(DEFAULT_FETCH_TIMEOUT_MS))
                .retryWhen(configureRetryPolicy())
                .flatMap(entities -> handleFetchResult(tracker, entities, startNanos));
    }

    /**
     * Safely converts a raw object fragment from the API response into a typed entity.
     *
     * @param fragment The raw object fragment received from the API.
     * @return The typed entity, or {@code null} if conversion fails.
     */
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

    /**
     * Configures the retry policy for the reactive chain using application properties.
     *
     * @return A configured {@link Retry} object.
     */
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

    /**
     * Handles the result of a fetch operation, updating the {@link ProcessorStatus} accordingly.
     *
     * @param tracker    The current {@link ProcessorStatus}.
     * @param entities   The list of fetched entities.
     * @param startNanos The start time of the fetch operation.
     * @return A {@link Mono} emitting the list of entities after status update.
     */
    private Mono<List<E>> handleFetchResult(ProcessorStatus tracker, List<E> entities, long startNanos) {
        if (entities.isEmpty()) {
            processorStatusService.resetAfterEmptyPage(tracker);
        } else {
            recentFailures = 0; // Reset circuit breaker failure count on successful fetch
            processorStatusService.advanceAfterBatch(tracker, entities, dateExtractorFunction());
        }
        recordMetrics(entities.size(), false, startNanos);
        return saveTracker(tracker).thenReturn(entities);
    }

    /**
     * Handles errors that occur during the fetch reactive chain, updating circuit breaker state and metrics.
     *
     * @param error      The {@link Throwable} that occurred.
     * @param startNanos The start time of the fetch operation.
     * @return A {@link Mono} emitting an empty list to allow the chain to continue gracefully.
     */
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

    /**
     * Records metrics for a fetch cycle (duration, success, error, etc.).
     *
     * @param itemCount  The number of items fetched.
     * @param isError    Whether an error occurred during the fetch.
     * @param startNanos The start time of the fetch operation in nanoseconds.
     */
    private void recordMetrics(int itemCount, boolean isError, long startNanos) {
        long elapsedNanos = System.nanoTime() - startNanos;
        if (fetchTimer != null) {
            fetchTimer.record(elapsedNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
        eventPublisher.publishEvent(new FetchCycleEvent(getProcessorType(), itemCount, itemCount == 0 && !isError, isError, elapsedNanos));
    }

    // --- Entity Processing (GenericHandler Implementation) ---

    /**
     * Handles a batch of entities received from the message channel, processing them concurrently.
     * This method is part of the Spring Integration {@link org.springframework.integration.core.GenericHandler} contract.
     *
     * @param payload The list of entities to process.
     * @param headers The message headers (currently unused).
     * @return {@code null} as processing is fire-and-forget (asynchronous).
     */
    @Override
    public Object handle(List<E> payload, MessageHeaders headers) {
        if (payload == null || payload.isEmpty()) return null;
        Flux.fromIterable(payload)
                .limitRate(applicationProperties.getProcessing().bulkhead().limitRate())
                .flatMap(this::processSingleEntity, processingConcurrency)
                .subscribe(); // Fire and forget
        return null;
    }

    /**
     * Processes a single entity, including formatting, event publishing, and notification.
     *
     * @param entity The entity to process.
     * @return A {@link Mono<Void>} that completes when processing is done.
     */
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

    // --- Delegate Methods for Ports/Domain Services ---

    /**
     * Retrieves or creates a {@link ProcessorStatus} from the {@link StatusTrackerStore}.
     *
     * @param resetOnStartup Whether to reset the tracker if it's the first poll.
     * @param pageSize       The configured page size.
     * @return A {@link Mono} emitting the {@link ProcessorStatus}.
     */
    protected Mono<ProcessorStatus> getOrCreateTracker(boolean resetOnStartup, long pageSize) {
        return statusTrackerStore.getOrCreate(getProcessorType(), resetOnStartup, pageSize);
    }

    /**
     * Saves the current {@link ProcessorStatus} to the {@link StatusTrackerStore}.
     *
     * @param tracker The {@link ProcessorStatus} to save.
     * @return A {@link Mono} emitting the saved {@link ProcessorStatus}.
     */
    protected Mono<ProcessorStatus> saveTracker(ProcessorStatus tracker) {
        return statusTrackerStore.save(tracker);
    }

    /**
     * Provides a function to extract the modified date string from an entity.
     *
     * @return A {@link Function} that maps an entity to its modified date string.
     */
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

    // --- Optional Hooks for Subclasses ---

    /**
     * Optional hook for subclasses to format or transform an entity before processing.
     * Defaults to returning the entity unchanged.
     *
     * @param entity The raw entity.
     * @return The formatted entity.
     */
    protected E doFormat(E entity) {
        return entity;
    }

    /**
     * Optional hook for subclasses to publish a secondary, derived event.
     * Defaults to a no-op.
     *
     * @param formatted The formatted entity.
     */
    protected void publishSecondaryEvent(E formatted) {
    }

    /**
     * Optional hook for subclasses to send a notification on successful processing.
     * Defaults to a no-op.
     *
     * @param formatted The formatted entity.
     * @param ctx       A context map with additional information.
     */
    protected void notifySuccess(E formatted, Map<String, Object> ctx) {
    }

    /**
     * Optional hook for subclasses to send a notification on processing error.
     * Defaults to a no-op.
     *
     * @param original The original entity before formatting.
     * @param e        The exception that was thrown.
     * @param ctx      A context map with additional information.
     */
    protected void notifyError(E original, Exception e, Map<String, Object> ctx) {
    }

    // --- Internal Utility Methods ---

    /**
     * Builds a filtered map of query parameters from a search request object.
     * Removes null or blank values.
     *
     * @param searchRequest The search request object.
     * @return A map of filtered query parameters.
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
     * Checks if a given {@link Throwable} represents a transient error that warrants a retry.
     *
     * @param t The {@link Throwable} to check.
     * @return {@code true} if the error is transient, {@code false} otherwise.
     */
    private boolean isTransientError(Throwable t) {
        return t instanceof TimeoutException || t instanceof IOException;
    }

    /**
     * Checks if the circuit breaker is currently open for this processor.
     *
     * @return {@code true} if the circuit breaker is open, {@code false} otherwise.
     */
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

    /**
     * Safely retrieves a method from an object's class.
     *
     * @param o    The object.
     * @param name The name of the method.
     * @return The {@link Method} object, or {@code null} if not found.
     */
    private Method safeMethod(Object o, String name) {
        try {
            return o.getClass().getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Invokes a method on an object and returns the result as a String.
     *
     * @param o The object to invoke the method on.
     * @param m The {@link Method} to invoke.
     * @return The String result of the invocation, or {@code null} if an error occurs.
     */
    private String invokeString(Object o, Method m) {
        try {
            return m == null ? null : (String) m.invoke(o);
        } catch (Exception e) {
            return null;
        }
    }
}
