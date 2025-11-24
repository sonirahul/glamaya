package com.glamaya.glamayawoocommercesync.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooOrderFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.datacontracts.woocommerce.OrderOrderBy;
import com.glamaya.datacontracts.woocommerce.OrderSearchRequest;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.glamayawoocommercesync.config.ApplicationProperties;
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatus;
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatusService;
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import com.glamaya.glamayawoocommercesync.port.out.EventPublisher;
import com.glamaya.glamayawoocommercesync.port.out.OAuthSignerPort;
import com.glamaya.glamayawoocommercesync.port.out.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.port.out.WooCommerceApiClientPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class OrderProcessor extends AbstractWooProcessor<Order> {

    private final EventPublisher eventPublisher;
    private final ContactMapperFactory<Order> contactMapperFactory;
    private final WooOrderFormatter wooOrderFormatter;
    private final N8nApplicationService n8nApplicationService;
    private final ApplicationProperties.ProcessorConfig orderConfig;

    public OrderProcessor(
            ObjectMapper objectMapper,
            PollerMetadata poller,
            OAuthSignerPort oAuth1Service,
            StatusTrackerStore statusTrackerStore,
            ProcessorStatusService processorStatusService,
            WooCommerceApiClientPort wooCommerceApiClient, // New parameter
            EventPublisher eventPublisher,
            ContactMapperFactory<Order> contactMapperFactory,
            WooOrderFormatter wooOrderFormatter,
            N8nApplicationService n8nApplicationService,
            ApplicationEventPublisher eventPublisherPublisher,
            ApplicationProperties applicationProperties,
            MeterRegistry meterRegistry) {
        super(
                objectMapper,
                poller,
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).pageSize(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).resetOnStartup(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).fetchDurationMs().active(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).fetchDurationMs().passive(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).queryUrl(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).enable(),
                applicationProperties.getProcessing().concurrency(),
                oAuth1Service,
                statusTrackerStore,
                processorStatusService,
                wooCommerceApiClient, // Pass to super
                eventPublisherPublisher,
                applicationProperties,
                meterRegistry
        );
        this.eventPublisher = eventPublisher;
        this.contactMapperFactory = contactMapperFactory;
        this.wooOrderFormatter = wooOrderFormatter;
        this.n8nApplicationService = n8nApplicationService;
        this.orderConfig = applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER);
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_ORDER;
    }

    @Override
    protected Object buildSearchRequest(ProcessorStatus tracker) {
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
            n8nApplicationService.success(true, orderConfig.n8n().webhookUrl(), formatted, ctx);
        }
    }

    @Override
    protected void notifyError(Order original, Exception e, Map<String, Object> ctx) {
        log.error("Order processing failed orderId={} errorMsg={}", original.getId(), e.getMessage(), e);
        if (orderConfig.n8n().enable()) {
            n8nApplicationService.error(true, orderConfig.n8n().errorWebhookUrl(),
                    "Error processing order: " + original.getId() + ", exception: " + e.getMessage(), ctx);
        }
    }
}
