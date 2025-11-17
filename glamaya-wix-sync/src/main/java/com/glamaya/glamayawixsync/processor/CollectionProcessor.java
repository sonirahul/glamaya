package com.glamaya.glamayawixsync.processor;

import com.glamaya.datacontracts.wix.Collection;
import com.glamaya.datacontracts.wix.CollectionQuery;
import com.glamaya.datacontracts.wix.CollectionQueryRequest;
import com.glamaya.datacontracts.wix.CollectionQueryResponse;
import com.glamaya.datacontracts.wix.Paging;
import com.glamaya.glamayawixsync.config.kafka.KafkaProducer;
import com.glamaya.glamayawixsync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawixsync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawixsync.repository.entity.ProcessorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionProcessor implements GlamWixProcessor<List<Collection>, CollectionQueryRequest, CollectionQueryResponse> {

    private final WebClient webClient;
    private final KafkaProducer<Collection> producer;
    @Getter
    private final PollerMetadata poller;
    private final ProcessorStatusTrackerRepository repository;

    @Value("${application.wix.entities.collections.query-url}")
    private String queryCollectionsUrl;
    @Value("${application.wix.entities.collections.enable}")
    private boolean enable;
    @Value("${application.wix.entities.collections.fetch-limit}")
    private long fetchLimit;
    @Value("${application.wix.entities.collections.reset-on-startup: false}")
    private boolean resetOnStartup;
    @Value("${application.kafka.topic.collection-events}")
    private String collectionEventsTopic;
    @Value("${application.wix.entities.collections.fetch-duration-in-millis.active-mode}")
    private int fetchDurationInMillisActiveMode;
    @Value("${application.wix.entities.collections.fetch-duration-in-millis.passive-mode}")
    private int fetchDurationInMillisPassiveMode;

    @Override
    public MessageSource<List<Collection>> receive() {
        return () -> {

            if (!enable) {
                log.info("Collection sync is disabled");
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode * 60);
                return null;
            }

            ProcessorStatusTracker statusTracker = getOrCreateStatusTracker(resetOnStartup, fetchLimit);
            resetOnStartup = false;

            var request = buildCollectionQueryRequest(statusTracker);

            var response = fetchEntities(webClient, queryCollectionsUrl, request, new ParameterizedTypeReference<>() {});

            var isResponseNullOrEmpty = response == null || response.getCollections() == null || response.getCollections().isEmpty();
            if (isResponseNullOrEmpty) {
                log.info("No new wix collections found, resetting status tracker offset and switching to passive mode");
                // As Wix API does not support filtering and sorting by updatedDate, we can use paging to fetch the latest collections
                //resetStatusTracker(statusTracker);
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode);
            } else {
                log.info("Fetched {} wix collections", statusTracker.getCount() + response.getCollections().size());
                updateStatusTracker(statusTracker, response.getCollections(), null, c -> ((Collection)c).getLastUpdated());
                modifyPollerDuration(poller, fetchDurationInMillisActiveMode);
            }
            repository.save(statusTracker).block();
            return isResponseNullOrEmpty ? null : MessageBuilder.withPayload(response.getCollections()).build();
        };
    }

    // as Wix API does not support filtering and sorting by updatedDate, we can use paging to fetch the latest collections
    private CollectionQueryRequest buildCollectionQueryRequest(ProcessorStatusTracker statusTracker) {
        var collectionQuery = CollectionQuery.builder()
                //.withSort(List.of(Sort.builder().withFieldName("updatedDate").withOrder(SortOrder.ASC).build()))
                .withPaging(Paging.builder().withLimit(fetchLimit).withOffset(statusTracker.getOffset()).build());

        /*if (statusTracker.getOffset() == 0 && statusTracker.getLastUpdatedDate() != null) {
            Map<String, Map<String, String>> filterMap = new HashMap<>();
            Map<String, String> dateMap = new HashMap<>();
            dateMap.put("$gt", statusTracker.getLastUpdatedDate().toString());
            filterMap.put("updatedDate", dateMap);
            collectionQuery.withFilter(filterMap);
        }*/

        return CollectionQueryRequest.builder()
                .withQuery(collectionQuery.build())
                .withIncludeNumberOfProducts(true)
                .withIncludeDescription(true)
                .build();
    }

    @Override
    public ProcessorStatusTrackerRepository getStatusTrackerRepository() {
        return repository;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WIX_COLLECTION;
    }

    @Override
    public Object handle(List<Collection> payload, MessageHeaders headers) {
        payload.forEach(product -> producer.send(collectionEventsTopic, product.getId(), product));
        return null;
    }
}