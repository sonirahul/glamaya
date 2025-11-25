package com.glamaya.glamayawoocommercesync.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.woocommerce.Product;
import com.glamaya.datacontracts.woocommerce.ProductOrderBy;
import com.glamaya.datacontracts.woocommerce.ProductSearchRequest;
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
 * Application service responsible for processing WooCommerce Product entities.
 * This class extends {@link AbstractApplicationService} to provide product-specific
 * logic for building search requests, extracting entity IDs, and publishing events.
 */
@Slf4j
@Service
public class ProductProcessor extends AbstractApplicationService<Product> {

    private final EventPublisher eventPublisher;
    private final N8nApplicationService n8nApplicationService;
    private final ApplicationProperties.ProcessorConfig productConfig;

    /**
     * Constructs a new {@code ProductProcessor}.
     *
     * @param objectMapper            The {@link ObjectMapper} for JSON serialization/deserialization.
     * @param oAuth1Service           The {@link OAuthSignerPort} for OAuth1 signature generation.
     * @param statusTrackerStore      The {@link StatusTrackerStore} for managing processor status.
     * @param processorStatusService  The {@link ProcessorStatusService} for domain-specific status logic.
     * @param wooCommerceApiClient    The {@link WooCommerceApiClientPort} for making API calls.
     * @param eventPublisher          The {@link EventPublisher} for publishing Kafka events.
     * @param n8nApplicationService   The {@link N8nApplicationService} for sending notifications.
     * @param eventPublisherPublisher The Spring {@link ApplicationEventPublisher} for internal events.
     * @param applicationProperties   The application's configuration properties.
     * @param meterRegistry           The {@link MeterRegistry} for metrics.
     */
    public ProductProcessor(
            ObjectMapper objectMapper,
            OAuthSignerPort oAuth1Service,
            StatusTrackerStore statusTrackerStore,
            ProcessorStatusService processorStatusService,
            WooCommerceApiClientPort wooCommerceApiClient,
            EventPublisher eventPublisher,
            N8nApplicationService n8nApplicationService,
            ApplicationEventPublisher eventPublisherPublisher,
            ApplicationProperties applicationProperties,
            MeterRegistry meterRegistry) {
        super(
                objectMapper,
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).pageSize(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).resetOnStartup(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).queryUrl(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).enable(),
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
        this.n8nApplicationService = n8nApplicationService;
        this.productConfig = applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT);
    }

    /**
     * Returns the processor type for products.
     *
     * @return {@link ProcessorType#WOO_PRODUCT}.
     */
    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_PRODUCT;
    }

    /**
     * Builds a {@link ProductSearchRequest} based on the current {@link ProcessorStatus}.
     *
     * @param tracker The current status tracker.
     * @return A configured {@link ProductSearchRequest} object.
     */
    @Override
    protected Object buildSearchRequest(ProcessorStatus tracker) {
        var b = ProductSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(ProductOrderBy.date_modified)
                .withOrder(SortOrder.asc)
                .withPage((long) tracker.getPage())
                .withPerPage(pageSize);
        if (tracker.isUseLastUpdatedDateInQuery() && tracker.getLastUpdatedDate() != null) {
            b.withModifiedAfter(tracker.getLastUpdatedDate());
        }
        var request = b.build();
        log.debug("Built product search request: page={} perPage={} lastUpdatedDate={} useLastUpdatedFlag={}", tracker.getPage(), pageSize, tracker.getLastUpdatedDate(), tracker.isUseLastUpdatedDateInQuery());
        return request;
    }

    /**
     * Returns the class type for WooCommerce Product entities.
     *
     * @return {@code Product.class}.
     */
    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    /**
     * Extracts the ID from a {@link Product} entity.
     *
     * @param entity The product entity.
     * @return The ID of the product.
     */
    @Override
    protected Object getEntityId(Product entity) {
        return entity.getId();
    }

    /**
     * Publishes the primary Kafka event for a formatted product.
     *
     * @param formatted The formatted product entity.
     */
    @Override
    protected void publishPrimaryEvent(Product formatted) {
        log.debug("Publishing primary product event productId={}", formatted.getId());
        eventPublisher.send(productConfig.kafkaTopic(), formatted.getId(), formatted);
    }

    /**
     * Sends a success notification to n8n if enabled.
     *
     * @param formatted The formatted product entity.
     * @param ctx       Context map for the notification.
     */
    @Override
    protected void notifySuccess(Product formatted, Map<String, Object> ctx) {
        log.debug("Product processed successfully productId={}", formatted.getId());
        if (productConfig.n8n().enable())
            n8nApplicationService.success(true, productConfig.n8n().webhookUrl(), formatted, ctx);
    }

    /**
     * Sends an error notification to n8n if enabled.
     *
     * @param original The original product entity.
     * @param e        The exception that occurred.
     * @param ctx      Context map for the notification.
     */
    @Override
    protected void notifyError(Product original, Exception e, Map<String, Object> ctx) {
        log.error("Product processing failed productId={} errorMsg={}", original.getId(), e.getMessage(), e);
        if (productConfig.n8n().enable())
            n8nApplicationService.error(true, productConfig.n8n().errorWebhookUrl(), e.getMessage(), ctx);
    }
}
