package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.glamayawoocommercesync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

public interface GlamWoocommerceProcessor<T> extends GenericHandler<T> {

    Logger LOG = LoggerFactory.getLogger(GlamWoocommerceProcessor.class);

    MessageSource<T> receive();

    PollerMetadata getPoller();

    ObjectMapper getObjectMapper();

    ProcessorStatusTrackerRepository getStatusTrackerRepository();

    ProcessorType getProcessorType();

    default Mono<ProcessorStatusTracker> getOrCreateStatusTracker(boolean resetOnStartup, long pageSize) {
        if (resetOnStartup) {
            return createStatusTracker(pageSize);
        }
        return getStatusTrackerRepository().findById(getProcessorType())
                .switchIfEmpty(createStatusTracker(pageSize))
                .flatMap(tracker -> {
                    if (tracker.getPageSize() != pageSize) {
                        return createStatusTracker(pageSize);
                    }
                    return Mono.just(tracker);
                });
    }

    private Mono<ProcessorStatusTracker> createStatusTracker(long pageSize) {
        return getStatusTrackerRepository().save(
                ProcessorStatusTracker.builder()
                        .processorType(getProcessorType())
                        .page(1)
                        .pageSize(pageSize)
                        .count(0)
                        .useLastUpdatedDateInQuery(false)
                        .build());
    }

    default void resetStatusTracker(ProcessorStatusTracker statusTracker) {
        statusTracker.setPage(1);
        statusTracker.setUseLastUpdatedDateInQuery(true);
    }

    default void updateStatusTracker(ProcessorStatusTracker statusTracker, List<?> response,
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

    // Change to reactive fetch
    default <E> Mono<List<E>> fetchEntities(WebClient webClient, String queryUrl, Map<String, String> queryParamMap,
                                            String oauthHeader, ParameterizedTypeReference<List<E>> typeRef) {
        String paramsDesc = (queryParamMap != null && !queryParamMap.isEmpty()) ?
                queryParamMap.entrySet().stream()
                        .map(e -> e.getKey() + "=" + (e.getKey().toLowerCase().contains("secret") || e.getKey().toLowerCase().contains("key") ? "***" : e.getValue()))
                        .reduce((a, b) -> a + "," + b).orElse("") : "";

        // Attempt to derive element class for direct mapping
        Class<?> elementClass = Object.class;
        try {
            ResolvableType rt = ResolvableType.forType(typeRef.getType());
            elementClass = rt.getGeneric(0).toClass();
        } catch (Exception ignored) {}
        Class<E> castClass = (Class<E>) elementClass;

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(queryUrl);
                    if (queryParamMap != null) {
                        queryParamMap.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, oauthHeader == null ? "" : oauthHeader)
                .retrieve()
                .bodyToFlux(castClass)
                .cast((Class<E>) castClass)
                .collectList()
                .doOnNext(list -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Fetched {} items for {}?{}", list.size(), queryUrl, paramsDesc);
                    }
                })
                .onErrorResume(e -> {
                    LOG.error("Error fetching entities from {}?{}: {}", queryUrl, paramsDesc, e.toString());
                    return Mono.empty();
                });
    }

    // Make asynchronous fire-and-forget publish (no blocking) with context logging
    default void publishData(WebClient webClient, String queryUrl, Object data, boolean n8nEnable, Map<String, Object> context) {
        if (!n8nEnable) {
            return;
        }
        webClient.post()
                .uri(queryUrl)
                .bodyValue(data)
                .retrieve()
                .toEntity(Void.class)
                .doOnSuccess(resp -> {
                    if (resp != null && resp.getStatusCode().is2xxSuccessful()) {
                        LOG.debug("n8n publish success url={} status={} ctx={}", queryUrl, resp.getStatusCode().value(), context);
                    } else if (resp != null) {
                        LOG.error("n8n publish non-2xx url={} status={} ctx={}", queryUrl, resp.getStatusCode().value(), context);
                    }
                })
                .doOnError(e -> LOG.error("n8n publish failed url={} error={} ctx={}", queryUrl, e.toString(), context))
                .onErrorResume(e -> Mono.empty())
                .subscribe(); // fire and forget
    }

    default Consumer<SourcePollingChannelAdapterSpec> poll() {
        return e -> e.poller(getPoller());
    }

    default void modifyPollerDuration(PollerMetadata pollerMetadata, int fetchDurationInMillisPassiveMode) {
        DynamicPeriodicTrigger trigger = (DynamicPeriodicTrigger) pollerMetadata.getTrigger();
        trigger.setDuration(Duration.ofMillis(fetchDurationInMillisPassiveMode));
    }
}