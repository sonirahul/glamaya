package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooOrderFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.datacontracts.woocommerce.OrderOrderBy;
import com.glamaya.datacontracts.woocommerce.OrderSearchRequest;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.glamayawoocommercesync.config.kafka.KafkaProducer;
import com.glamaya.glamayawoocommercesync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import com.glamaya.glamayawoocommercesync.service.OAuth1Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessor implements GlamWoocommerceProcessor<List<Order>> {

    @Qualifier("woocommerceWebClient")
    private final WebClient woocommerceWebClient;
    @Qualifier("n8nWebhookWebClient")
    private final WebClient n8nWebClient;
    private final OAuth1Service oAuth1Service;
    private final KafkaProducer<Object> producer;
    @Getter
    private final PollerMetadata poller;
    @Getter
    private final ObjectMapper objectMapper;
    private final ProcessorStatusTrackerRepository repository;
    private final ContactMapperFactory<Order> contactMapperFactory;
    private final WooOrderFormatter wooOrderFormatter;

    @Value("${application.woocommerce.entities.orders.query-url}")
    private final String queryOrdersUrl;
    @Value("${application.woocommerce.entities.orders.n8n.webhook-url}")
    private final String n8nWebhookUrl;
    @Value("${application.woocommerce.entities.orders.n8n.error-webhook-url}")
    private final String n8nErrorWebhookUrl;
    @Value("${application.woocommerce.entities.orders.n8n.enable}")
    private final boolean n8nEnable;
    @Value("${application.woocommerce.entities.orders.enable}")
    private final boolean enable;
    @Value("${application.woocommerce.entities.orders.page-size}")
    private final long pageSize;
    @Value("${application.woocommerce.entities.orders.reset-on-startup}")
    private boolean resetOnStartup;
    @Value("${external.woocommerce.api.account-name}")
    private final String sourceAccountName;
    @Value("${application.kafka.topic.order-events}")
    private final String orderEventsTopic;
    @Value("${application.kafka.topic.contact-events}")
    private final String contactEventsTopic;
    @Value("${application.woocommerce.entities.orders.fetch-duration-in-millis.active-mode}")
    private int fetchDurationInMillisActiveMode;
    @Value("${application.woocommerce.entities.orders.fetch-duration-in-millis.passive-mode}")
    private int fetchDurationInMillisPassiveMode;

    public MessageSource<List<Order>> receive() {
        return () -> {
            if (!enable) {
                log.info("Order sync is disabled");
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode * 60);
                return null;
            }

            Mono<ProcessorStatusTracker> trackerMono = getOrCreateStatusTracker(resetOnStartup, pageSize)
                    .doOnNext(t -> resetOnStartup = false);

            return trackerMono.flatMap(statusTracker -> {
                        OrderSearchRequest orderSearchRequest = buildOrderSearchRequest(statusTracker);
                        Map<String, String> queryParamMap = objectMapper.convertValue(orderSearchRequest, new TypeReference<>() {});
                        String oauthHeader = oAuth1Service.generateOAuth1Header(queryOrdersUrl, queryParamMap);
                        return fetchEntities(woocommerceWebClient, queryOrdersUrl, queryParamMap, oauthHeader, new ParameterizedTypeReference<List<Order>>() {})
                                .defaultIfEmpty(List.of())
                                .flatMap(orders -> {
                                    if (orders.isEmpty()) {
                                        log.info("No new woocommerce Orders found, resetting status tracker and switching to passive mode");
                                        resetStatusTracker(statusTracker);
                                        modifyPollerDuration(poller, fetchDurationInMillisPassiveMode);
                                    } else {
                                        log.info("Fetched {} woocommerce Orders", statusTracker.getCount() + orders.size());
                                        updateStatusTracker(statusTracker, orders, o -> ((Order) o).getDateModifiedGmt());
                                        modifyPollerDuration(poller, fetchDurationInMillisActiveMode);
                                    }
                                    return repository.save(statusTracker)
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .thenReturn(orders);
                                });
                    })
                    .map(orders -> orders.isEmpty() ? null : MessageBuilder.withPayload(orders).build())
                    .block(); // boundary sync
        };
    }

    private OrderSearchRequest buildOrderSearchRequest(ProcessorStatusTracker statusTracker) {
        var builder = OrderSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(OrderOrderBy.date_modified)
                .withOrder(SortOrder.asc)
                .withPage(statusTracker.getPage())
                .withPerPage(pageSize);

        if (statusTracker.isUseLastUpdatedDateInQuery() && statusTracker.getLastUpdatedDate() != null) {
            builder.withModifiedAfter(statusTracker.getLastUpdatedDate());
        }
        return builder.build();
    }

    @Override
    public ProcessorStatusTrackerRepository getStatusTrackerRepository() {
        return repository;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_ORDER;
    }

    @Override
    public Consumer<SourcePollingChannelAdapterSpec> poll() {
        return e -> e.poller(getPoller());
    }

    @Override
    public Object handle(List<Order> payload, MessageHeaders headers) {
        payload.forEach(order -> {
            try {
                order = wooOrderFormatter.format(order);
                producer.send(orderEventsTopic, order.getId().toString(), order);
                var contact = contactMapperFactory.toGlamayaContact(order, sourceAccountName);
                producer.send(contactEventsTopic, contact.getId(), contact);
                Map<String,Object> ctx = new HashMap<>();
                ctx.put("entity","order");
                ctx.put("id", order.getId());
                publishData(n8nWebClient, n8nWebhookUrl, order, n8nEnable, ctx);
            } catch (Exception e) {
                Map<String,Object> errCtx = new HashMap<>();
                errCtx.put("entity","order");
                errCtx.put("id", order.getId());
                errCtx.put("error", e.getMessage());
                log.error("Error processing order: {}", order.getId(), e);
                publishData(n8nWebClient, n8nErrorWebhookUrl, "Error processing order: " + order.getId() + ", exception: " + e.getMessage(), n8nEnable, errCtx);
            }
        });
        return null;
    }
}