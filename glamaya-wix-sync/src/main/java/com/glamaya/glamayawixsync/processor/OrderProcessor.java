package com.glamaya.glamayawixsync.processor;

import com.glamaya.datacontracts.wix.CursorPaging;
import com.glamaya.datacontracts.wix.Order;
import com.glamaya.datacontracts.wix.OrderQueryRequest;
import com.glamaya.datacontracts.wix.OrderQueryResponse;
import com.glamaya.datacontracts.wix.Search;
import com.glamaya.datacontracts.wix.Sort;
import com.glamaya.datacontracts.wix.SortOrder;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessor implements GlamWixProcessor<List<Order>, OrderQueryRequest, OrderQueryResponse> {

    private final WebClient webClient;
    private final KafkaProducer<Order> producer;
    @Getter
    private final PollerMetadata poller;
    private final ProcessorStatusTrackerRepository repository;

    @Value("${application.wix.entities.orders.query-url}")
    private final String queryOrdersUrl;
    @Value("${application.wix.entities.orders.enable}")
    private final boolean enable;
    @Value("${application.wix.entities.orders.fetch-limit}")
    private final long fetchLimit;
    @Value("${application.wix.entities.orders.reset-on-startup: false}")
    private boolean resetOnStartup;
    @Value("${application.kafka.topic.order-events}")
    private final String orderEventsTopic;
    @Value("${application.wix.entities.orders.fetch-duration-in-millis.active-mode}")
    private int fetchDurationInMillisActiveMode;
    @Value("${application.wix.entities.orders.fetch-duration-in-millis.passive-mode}")
    private int fetchDurationInMillisPassiveMode;

    @Override
    public MessageSource<List<Order>> receive() {
        return () -> {

            if (!enable) {
                log.info("Order sync is disabled");
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode * 60);
                return null;
            }

            ProcessorStatusTracker statusTracker = getOrCreateStatusTracker(resetOnStartup, fetchLimit);
            resetOnStartup = false; // Reset only once on startup

            var request = buildOrderQueryRequest(statusTracker);

            var response = fetchEntities(webClient, queryOrdersUrl, request, new ParameterizedTypeReference<>() {});

            var isResponseNullOrEmpty = response == null || response.getOrders() == null || response.getOrders().isEmpty();
            if (isResponseNullOrEmpty) {
                log.info("No new wix orders found, resetting status tracker and switching to passive mode");
                resetStatusTracker(statusTracker);
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode);
            } else {
                log.info("Fetched {} wix orders", statusTracker.getCount() + response.getOrders().size());
                updateStatusTracker(statusTracker, response.getOrders(), response.getMetadata(), c -> ((Order) c).getUpdatedDate());
                modifyPollerDuration(poller, fetchDurationInMillisActiveMode);
            }
            repository.save(statusTracker).block();
            return isResponseNullOrEmpty ? null : MessageBuilder.withPayload(response.getOrders()).build();
        };
    }

    private OrderQueryRequest buildOrderQueryRequest(ProcessorStatusTracker statusTracker) {
        var cursorPaging = CursorPaging.builder().withLimit(fetchLimit).build();
        if (StringUtils.hasText(statusTracker.getCursor())) {
            cursorPaging.setCursor(statusTracker.getCursor());
        }

        var query = Search.builder()
                .withSort(List.of(Sort.builder().withFieldName("updatedDate").withOrder(SortOrder.ASC).build()))
                .withCursorPaging(cursorPaging).build();

        if (!StringUtils.hasText(statusTracker.getCursor()) && statusTracker.getLastUpdatedDate() != null) {
            Map<String, Map<String, String>> filterMap = new HashMap<>();
            Map<String, String> dateMap = new HashMap<>();
            dateMap.put("$gt", statusTracker.getLastUpdatedDate().toString());
            filterMap.put("updatedDate", dateMap);
            query.setFilter(filterMap);
        }

        return OrderQueryRequest.builder()
                .withSearch(query)
                .build();
    }

    @Override
    public ProcessorStatusTrackerRepository getStatusTrackerRepository() {
        return repository;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WIX_ORDER;
    }

    @Override
    public Object handle(List<Order> payload, MessageHeaders headers) {
        payload.forEach(order -> producer.send(orderEventsTopic, order.getId(), order));
        return null;
    }
}
