package com.glamaya.glamayawixsync.processor;

import com.glamaya.datacontracts.wix.Metadata;
import com.glamaya.glamayawixsync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawixsync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawixsync.repository.entity.ProcessorType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public interface GlamWixProcessor<T, I, O> extends GenericHandler<T> {

    MessageSource<T> receive();

    PollerMetadata getPoller();

    ProcessorStatusTrackerRepository getStatusTrackerRepository();

    ProcessorType getProcessorType();

    default ProcessorStatusTracker getOrCreateStatusTracker(boolean resetOnStartup, long fetchLimit) {
        if (resetOnStartup) {
            return createStatusTracker(fetchLimit).block();
        }

        var statusTracker = getStatusTrackerRepository().findById(getProcessorType())
                .switchIfEmpty(createStatusTracker(fetchLimit)).block();

        // Ensure that the status tracker is not null and has the correct fetch limit set
        if (statusTracker != null && statusTracker.getFetchLimit() != fetchLimit) {
            return createStatusTracker(fetchLimit).block();
        }
        return statusTracker;
    }

    private Mono<ProcessorStatusTracker> createStatusTracker(long fetchLimit) {
        return getStatusTrackerRepository().save(
                ProcessorStatusTracker.builder()
                        .processorType(getProcessorType())
                        .offset(0L)
                        .fetchLimit(fetchLimit)
                        .count(0L)
                        .build());
    }

    default void resetStatusTracker(ProcessorStatusTracker statusTracker) {
        statusTracker.setOffset(0L);
    }

    default void updateStatusTracker(ProcessorStatusTracker statusTracker, List<?> response, Metadata metadata,
                                     Function<Object, Instant> getDateModified) {
        statusTracker.setCount(statusTracker.getCount() + response.size());
        statusTracker.setOffset(statusTracker.getOffset() + response.size());
        statusTracker.setLastUpdatedDate(getDateModified.apply(response.getLast()));

        // Update the cursor based on the metadata
        if (metadata != null) {
            if (metadata.getCursors() != null && StringUtils.hasText(metadata.getCursors().getNext())) {
                statusTracker.setCursor(metadata.getCursors().getNext());
            } else {
                statusTracker.setCursor(null);
                resetStatusTracker(statusTracker);
            }
        }
    }

    default O fetchEntities(WebClient webClient, String queryUrl,
                                      I request, ParameterizedTypeReference<O> typeRef) {

        return webClient.post()
                .uri(queryUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(typeRef)
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
