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
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Application service responsible for processing WooCommerce Order entities.
 * This class extends {@link AbstractApplicationService} to provide order-specific
 * logic for building search requests, extracting entity IDs, and publishing events.
 */
@Slf4j
@Service
public class OrderProcessor extends AbstractApplicationService<Order> {

    private final EventPublisher eventPublisher;
    private final ContactMapperFactory<Order> contactMapperFactory;
    private final WooOrderFormatter wooOrderFormatter;
    private final N8nApplicationService n8nApplicationService;
    private final ApplicationProperties.ProcessorConfig orderConfig;

    /**
     * Constructs a new {@code OrderProcessor}.
     *
     * @param objectMapper            The {@link ObjectMapper} for JSON serialization/deserialization.
     * @param oAuth1Service           The {@link OAuthSignerPort} for OAuth1 signature generation.
     * @param statusTrackerStore      The {@link StatusTrackerStore} for managing processor status.
     * @param processorStatusService  The {@link ProcessorStatusService} for domain-specific status logic.
     * @param wooCommerceApiClient    The {@link WooCommerceApiClientPort} for making API calls.
     * @param eventPublisher          The {@link EventPublisher} for publishing Kafka events.
     * @param contactMapperFactory    The {@link ContactMapperFactory} for mapping orders to contacts.
     * @param wooOrderFormatter       The {@link WooOrderFormatter} for formatting order data.
     * @param n8nApplicationService   The {@link N8nApplicationService} for sending notifications.
     * @param eventPublisherPublisher The Spring {@link ApplicationEventPublisher} for internal events.
     * @param applicationProperties   The application's configuration properties.
     * @param meterRegistry           The {@link MeterRegistry} for metrics.
     */
    public OrderProcessor(
            ObjectMapper objectMapper,
            OAuthSignerPort oAuth1Service,
            StatusTrackerStore statusTrackerStore,
            ProcessorStatusService processorStatusService,
            WooCommerceApiClientPort wooCommerceApiClient,
            EventPublisher eventPublisher,
            ContactMapperFactory<Order> contactMapperFactory,
            WooOrderFormatter wooOrderFormatter,
            N8nApplicationService n8nApplicationService,
            ApplicationEventPublisher eventPublisherPublisher,
            ApplicationProperties applicationProperties,
            MeterRegistry meterRegistry) {
        super(
                objectMapper,
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).pageSize(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).resetOnStartup(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).queryUrl(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_ORDER).enable(),
                applicationProperties.getProcessing().concurrency(),
                oAuth1Service,
                statusTrackerStore,
                processorStatusService,
                wooCommerceApiClient,
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

    /**
     * Returns the processor type for orders.
     *
     * @return {@link ProcessorType#WOO_ORDER}.
     */
    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_ORDER;
    }

    /**
     * Builds an {@link OrderSearchRequest} based on the current {@link ProcessorStatus}.
     *
     * @param tracker The current status tracker.
     * @return A configured {@link OrderSearchRequest} object.
     */
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
        log.debug("Built order search request: page={} perPage={} lastUpdatedDate={} useLastUpdatedFlag={}", tracker.getPage(), pageSize, tracker.getLastUpdatedDate(), tracker.isUseLastUpdatedDateInQuery());
        return request;
    }

    /**
     * Returns the class type for WooCommerce Order entities.
     *
     * @return {@code Order.class}.
     */
    @Override
    protected Class<Order> getEntityClass() {
        return Order.class;
    }

    /**
     * Formats the given {@link Order} entity using {@link WooOrderFormatter}.
     *
     * @param entity The raw order entity.
     * @return The formatted order entity.
     */
    @Override
    protected Order doFormat(Order entity) {
        return wooOrderFormatter.format(entity);
    }

    /**
     * Extracts the ID from an {@link Order} entity.
     *
     * @param entity The order entity.
     * @return The ID of the order.
     */
    @Override
    protected Object getEntityId(Order entity) {
        return entity.getId();
    }

    /**
     * Publishes the primary Kafka event for a formatted order.
     *
     * @param formatted The formatted order entity.
     */
    @Override
    protected void publishPrimaryEvent(Order formatted) {
        log.debug("Publishing primary order event orderId={}", formatted.getId());
        eventPublisher.send(orderConfig.kafkaTopic(), formatted.getId(), formatted);
    }

    /**
     * Publishes a secondary Kafka event for a formatted order, typically a contact event.
     *
     * @param formatted The formatted order entity.
     */
    @Override
    protected void publishSecondaryEvent(Order formatted) {
        log.debug("Publishing secondary order event orderId={}", formatted.getId());
        var contact = contactMapperFactory.toGlamayaContact(formatted, orderConfig.sourceAccountName());
        eventPublisher.send(orderConfig.contactKafkaTopic(), contact.getId(), contact);
    }

    /**
     * Sends a success notification to n8n if enabled.
     *
     * @param formatted The formatted order entity.
     * @param ctx       Context map for the notification.
     */
    @Override
    protected void notifySuccess(Order formatted, Map<String, Object> ctx) {
        log.debug("Order processed successfully orderId={}", formatted.getId());
        if (orderConfig.n8n().enable()) {
            n8nApplicationService.success(true, orderConfig.n8n().webhookUrl(), formatted, ctx);
        }
    }

    /**
     * Sends an error notification to n8n if enabled.
     *
     * @param original The original order entity.
     * @param e        The exception that occurred.
     * @param ctx      Context map for the notification.
     */
    @Override
    protected void notifyError(Order original, Exception e, Map<String, Object> ctx) {
        log.error("Order processing failed orderId={} errorMsg={}", original.getId(), e.getMessage(), e);
        if (orderConfig.n8n().enable()) {
            n8nApplicationService.error(true, orderConfig.n8n().errorWebhookUrl(),
                    "Error processing order: " + original.getId() + ", exception: " + e.getMessage(), ctx);
        }
    }
}
