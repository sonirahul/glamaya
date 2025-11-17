package com.glamaya.glamayawoocommercesync.processor;

import com.glamaya.glamayawoocommercesync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import org.springframework.core.ParameterizedTypeReference;
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

    MessageSource<T> receive();

    PollerMetadata getPoller();

    ProcessorStatusTrackerRepository getStatusTrackerRepository();

    ProcessorType getProcessorType();

    default ProcessorStatusTracker getOrCreateStatusTracker(boolean resetOnStartup, long pageSize) {
        if (resetOnStartup) {
            return createStatusTracker(pageSize).block();
        }

        var statusTracker = getStatusTrackerRepository().findById(getProcessorType())
                .switchIfEmpty(createStatusTracker(pageSize)).block();

        // Ensure that the status tracker is not null and has the correct page size
        if (statusTracker != null && statusTracker.getPageSize() != pageSize) {
            return createStatusTracker(pageSize).block();
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

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(queryUrl);
                    queryParamMap.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, oauthHeader)
                .retrieve()
                .bodyToMono(typeRef)
                .block();
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