package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.glamayawoocommercesync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    default ProcessorStatusTracker getOrCreateStatusTracker(boolean resetOnStartup, long pageSize) {
        if (resetOnStartup) {
            return createStatusTracker(pageSize).subscribeOn(Schedulers.boundedElastic()).block();
        }

        var statusTracker = getStatusTrackerRepository().findById(getProcessorType())
                .switchIfEmpty(createStatusTracker(pageSize))
                .subscribeOn(Schedulers.boundedElastic())
                .block();

        // Ensure that the status tracker is not null and has the correct page size
        if (statusTracker != null && statusTracker.getPageSize() != pageSize) {
            return createStatusTracker(pageSize).subscribeOn(Schedulers.boundedElastic()).block();
        }
        return statusTracker;
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

        long newCount = statusTracker.getCount() + response.size();
        statusTracker.setCount(newCount);
        statusTracker.setPage(statusTracker.getPage() + 1);
        String lastDateModified = getDateModified.apply(response.getLast());
        statusTracker.setLastUpdatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(lastDateModified));
    }

    default <E> List<E> fetchEntities(WebClient webClient, String queryUrl, Map<String, String> queryParamMap,
                                      String oauthHeader, ParameterizedTypeReference<List<E>> typeRef) {

        // Build a canonical request description for logs (mask common secrets)
        String paramsDesc;
        if (queryParamMap != null && !queryParamMap.isEmpty()) {
            paramsDesc = queryParamMap.entrySet().stream()
                    .map(e -> e.getKey() + "=" + (e.getKey().toLowerCase().contains("secret") || e.getKey().toLowerCase().contains("key") ? "***" : e.getValue()))
                    .reduce((a, b) -> a + "," + b).orElse("");
        } else {
            paramsDesc = "";
        }

        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(queryUrl);
                        if (queryParamMap != null) {
                            queryParamMap.forEach(uriBuilder::queryParam);
                        }
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, oauthHeader == null ? "" : oauthHeader)
                    .exchangeToMono(response -> response.bodyToMono(String.class)
                            .flatMap(body -> {
                                // Log debug of response size
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Received response (len={}) for {}?{}", body == null ? 0 : body.length(), queryUrl, paramsDesc);
                                }
                                if (!response.statusCode().is2xxSuccessful()) {
                                    LOG.error("Non-success response for request {}?{} status={} body={}",
                                            queryUrl, paramsDesc, response.statusCode().value(), body == null ? "<empty>" : body.substring(0, Math.min(body.length(), 2000)));
                                    return Mono.error(new RuntimeException("Non-success response: " + response.statusCode()));
                                }

                                try {
                                    JavaType javaType = getObjectMapper().getTypeFactory().constructType(typeRef.getType());
                                    List<E> result = getObjectMapper().readValue(body, javaType);
                                    return Mono.just(result);
                                } catch (Exception ex) {
                                    // Log detailed diagnostic to help debug mapping issues
                                    LOG.error("Failed to deserialize response for request {}?{}. HTTP status={}. Response body (first 2000 chars): {}",
                                            queryUrl, paramsDesc, response.statusCode().value(), body, ex);
                                    return Mono.error(new RuntimeException("Failed to deserialize Woocommerce response: see logs for body", ex));
                                }
                            }))
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();
        } catch (Exception e) {
            // Provide a helpful wrapper if anything else goes wrong
            LOG.error("Error fetching entities from {}?{}. Exception: {}", queryUrl, paramsDesc, e, e);
            throw e;
        }
    }

    default void publishData(WebClient webClient, String queryUrl, Object data, boolean n8nEnable) {

        if (!n8nEnable) {
            return;
        }

        webClient.post()
                .uri(queryUrl)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    default Consumer<SourcePollingChannelAdapterSpec> poll() {
        return e -> e.poller(getPoller());
    }

    default void modifyPollerDuration(PollerMetadata pollerMetadata, int fetchDurationInMillisPassiveMode) {
        DynamicPeriodicTrigger trigger = (DynamicPeriodicTrigger) pollerMetadata.getTrigger();
        trigger.setDuration(Duration.ofMillis(fetchDurationInMillisPassiveMode));
    }
}