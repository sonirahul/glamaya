package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooOrderFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.datacontracts.woocommerce.OrderOrderBy;
import com.glamaya.datacontracts.woocommerce.OrderSearchRequest;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.glamayawoocommercesync.config.ApplicationProperties;
import com.glamaya.glamayawoocommercesync.port.EventPublisher;
import com.glamaya.glamayawoocommercesync.port.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import com.glamaya.glamayawoocommercesync.service.N8nNotificationService;
import com.glamaya.glamayawoocommercesync.service.OAuth1Service;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Processor for WooCommerce Order entities.
 * Handles polling, event publishing, and notifications for orders.
 * Configuration is injected via ApplicationProperties.processors.orders.
 */
@Slf4j
@Service
public class OrderProcessor extends AbstractWooProcessor<Order> {
    private final EventPublisher eventPublisher;
    private final ContactMapperFactory<Order> contactMapperFactory;
    private final WooOrderFormatter wooOrderFormatter;
    private final N8nNotificationService n8nNotificationService;
    private final ApplicationProperties.ProcessorConfig orderConfig;

    public OrderProcessor(
            WebClient woocommerceWebClient,
            ObjectMapper objectMapper,
            PollerMetadata poller,
            OAuth1Service oAuth1Service,
            StatusTrackerStore statusTrackerStore,
            EventPublisher eventPublisher,
            ContactMapperFactory<Order> contactMapperFactory,
            WooOrderFormatter wooOrderFormatter,
            N8nNotificationService n8nNotificationService,
            ApplicationEventPublisher eventPublisherPublisher,
            ApplicationProperties applicationProperties,
            MeterRegistry meterRegistry) {
        super(
                woocommerceWebClient,
                objectMapper,
                poller,
                (long) applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).pageSize(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).resetOnStartup(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).fetchDurationMs().active(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).fetchDurationMs().passive(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).queryUrl(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).enable(),
                applicationProperties.getProcessing().concurrency(),
                oAuth1Service,
                statusTrackerStore,
                eventPublisherPublisher,
                applicationProperties,
                meterRegistry
        );
        this.eventPublisher = eventPublisher;
        this.contactMapperFactory = contactMapperFactory;
        this.wooOrderFormatter = wooOrderFormatter;
        this.n8nNotificationService = n8nNotificationService;
        this.orderConfig = applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER);
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
                .withPage((long) tracker.getPage())
                .withPerPage(pageSize);
        if (tracker.isUseLastUpdatedDateInQuery() && tracker.getLastUpdatedDate() != null) {
            b.withModifiedAfter(tracker.getLastUpdatedDate());
        }
        var request = b.build();
        if (log.isDebugEnabled()) {
            log.debug("Built order search request: page={} perPage={} lastUpdatedDate={} useLastUpdatedFlag={}", tracker.getPage(), pageSize, tracker.getLastUpdatedDate(), tracker.isUseLastUpdatedDateInQuery());
        }
        return request;
    }

    @Override
    protected Class<Order> getEntityClass() {
        return Order.class;
    }

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
        log.debug("Publishing primary order event orderId={}", formatted.getId());
        eventPublisher.send(orderConfig.kafkaTopic(), formatted.getId(), formatted);
    }

    @Override
    protected void publishSecondaryEvent(Order formatted) {
        log.debug("Publishing secondary order event orderId={}", formatted.getId());
        var contact = contactMapperFactory.toGlamayaContact(formatted, orderConfig.sourceAccountName());
        eventPublisher.send(orderConfig.contactKafkaTopic(), contact.getId(), contact);
    }

    @Override
    protected void notifySuccess(Order formatted, Map<String, Object> ctx) {
        log.debug("Order processed successfully orderId={}", formatted.getId());
        if (orderConfig.n8n().enable()) {
            n8nNotificationService.success(true, orderConfig.n8n().webhookUrl(), formatted, ctx);
        }
    }

    @Override
    protected void notifyError(Order original, Exception e, Map<String, Object> ctx) {
        log.error("Order processing failed orderId={} errorMsg={}", original.getId(), e.getMessage(), e);
        if (orderConfig.n8n().enable()) {
            n8nNotificationService.error(true, orderConfig.n8n().errorWebhookUrl(),
                    "Error processing order: " + original.getId() + ", exception: " + e.getMessage(), ctx);
        }
    }
}
