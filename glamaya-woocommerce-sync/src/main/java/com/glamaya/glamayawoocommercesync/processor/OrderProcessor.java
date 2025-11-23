package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooOrderFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.datacontracts.woocommerce.OrderOrderBy;
import com.glamaya.datacontracts.woocommerce.OrderSearchRequest;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.glamayawoocommercesync.port.EventPublisher;
import com.glamaya.glamayawoocommercesync.port.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import com.glamaya.glamayawoocommercesync.service.N8nNotificationService;
import com.glamaya.glamayawoocommercesync.service.OAuth1Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class OrderProcessor extends AbstractWooProcessor<Order> {

    private final EventPublisher eventPublisher;
    private final ContactMapperFactory<Order> contactMapperFactory;
    private final WooOrderFormatter wooOrderFormatter;
    private final N8nNotificationService n8nNotificationService;

    @Value("${application.kafka.topic.order-events}")
    private String orderEventsTopic;
    @Value("${application.kafka.topic.contact-events}")
    private String contactEventsTopic;
    @Value("${application.woocommerce.entities.orders.n8n.webhook-url}")
    private String n8nWebhookUrl;
    @Value("${application.woocommerce.entities.orders.n8n.error-webhook-url}")
    private String n8nErrorWebhookUrl;
    @Value("${application.woocommerce.entities.orders.n8n.enable}")
    private boolean n8nEnable;
    @Value("${external.woocommerce.api.account-name}")
    private String sourceAccountName;

    public OrderProcessor(WebClient woocommerceWebClient,
                          ObjectMapper objectMapper,
                          PollerMetadata poller,
                          @Value("${application.woocommerce.entities.orders.page-size}") long pageSize,
                          @Value("${application.woocommerce.entities.orders.reset-on-startup}") boolean resetOnStartup,
                          @Value("${application.woocommerce.entities.orders.fetch-duration-in-millis.active-mode}") int active,
                          @Value("${application.woocommerce.entities.orders.fetch-duration-in-millis.passive-mode}") int passive,
                          @Value("${application.woocommerce.entities.orders.query-url}") String queryUrl,
                          @Value("${application.woocommerce.entities.orders.enable}") boolean enable,
                          @Value("${application.processing.concurrency:4}") int processingConcurrency,
                          OAuth1Service oAuth1Service,
                          StatusTrackerStore statusTrackerStore,
                          EventPublisher eventPublisher,
                          ContactMapperFactory<Order> contactMapperFactory,
                          WooOrderFormatter wooOrderFormatter,
                          N8nNotificationService n8nNotificationService,
                          ApplicationEventPublisher eventPublisherPublisher) {
        super(woocommerceWebClient, objectMapper, poller, pageSize, resetOnStartup, active, passive, queryUrl, enable, processingConcurrency, oAuth1Service, statusTrackerStore, eventPublisherPublisher);
        this.eventPublisher = eventPublisher;
        this.contactMapperFactory = contactMapperFactory;
        this.wooOrderFormatter = wooOrderFormatter;
        this.n8nNotificationService = n8nNotificationService;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_ORDER;
    }

    @Override
    protected Object buildSearchRequest(ProcessorStatusTracker tracker) {
        var b = OrderSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(OrderOrderBy.date_modified)
                .withOrder(SortOrder.asc)
                .withPage(Long.valueOf(tracker.getPage()))
                .withPerPage(Long.valueOf(pageSize));
        if (tracker.isUseLastUpdatedDateInQuery() && tracker.getLastUpdatedDate() != null) {
            b.withModifiedAfter(tracker.getLastUpdatedDate());
        }
        return b.build();
    }

    @Override
    protected Class<Order> getEntityClass() {
        return Order.class;
    }

    // New hook implementations
    @Override
    protected Order doFormat(Order entity) {
        return wooOrderFormatter.format(entity);
    }

    @Override
    protected Object getEntityId(Order entity) {
        return entity.getId();
    }

    @Override
    protected void publishPrimaryEvent(Order formatted) {
        eventPublisher.send(orderEventsTopic, formatted.getId(), formatted);
    }

    @Override
    protected void publishSecondaryEvent(Order formatted) {
        var contact = contactMapperFactory.toGlamayaContact(formatted, sourceAccountName);
        eventPublisher.send(contactEventsTopic, contact.getId(), contact);
    }

    @Override
    protected void notifySuccess(Order formatted, Map<String, Object> ctx) {
        if (n8nEnable) n8nNotificationService.success(true, n8nWebhookUrl, formatted, ctx);
    }

    @Override
    protected void notifyError(Order original, Exception e, Map<String, Object> ctx) {
        if (n8nEnable)
            n8nNotificationService.error(true, n8nErrorWebhookUrl, "Error processing order: " + original.getId() + ", exception: " + e.getMessage(), ctx);
    }
}
