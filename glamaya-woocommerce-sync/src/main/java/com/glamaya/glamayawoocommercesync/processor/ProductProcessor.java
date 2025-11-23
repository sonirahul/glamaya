package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.woocommerce.Product;
import com.glamaya.datacontracts.woocommerce.ProductOrderBy;
import com.glamaya.datacontracts.woocommerce.ProductSearchRequest;
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

@Slf4j
@Service
public class ProductProcessor extends AbstractWooProcessor<Product> {
    private final EventPublisher eventPublisher;
    private final N8nNotificationService n8nNotificationService;
    private final ApplicationProperties.ProcessorConfig productConfig;

    public ProductProcessor(
            WebClient woocommerceWebClient,
            ObjectMapper objectMapper,
            PollerMetadata poller,
            OAuth1Service oAuth1Service,
            StatusTrackerStore statusTrackerStore,
            EventPublisher eventPublisher,
            N8nNotificationService n8nNotificationService,
            ApplicationEventPublisher eventPublisherPublisher,
            ApplicationProperties applicationProperties,
            MeterRegistry meterRegistry) {
        super(
                woocommerceWebClient,
                objectMapper,
                poller,
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).pageSize(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).resetOnStartup(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).fetchDurationMs().active(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).fetchDurationMs().passive(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).queryUrl(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT).enable(),
                applicationProperties.getProcessing().concurrency(),
                oAuth1Service,
                statusTrackerStore,
                eventPublisherPublisher,
                applicationProperties,
                meterRegistry
        );
        this.eventPublisher = eventPublisher;
        this.n8nNotificationService = n8nNotificationService;
        this.productConfig = applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_PRODUCT);
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_PRODUCT;
    }

    @Override
    protected Object buildSearchRequest(ProcessorStatusTracker tracker) {
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
        if (log.isDebugEnabled()) {
            log.debug("Built product search request: page={} perPage={} lastUpdatedDate={} useLastUpdatedFlag={}", tracker.getPage(), pageSize, tracker.getLastUpdatedDate(), tracker.isUseLastUpdatedDateInQuery());
        }
        return request;
    }

    @Override
    protected Product doFormat(Product entity) {
        return entity;
    }

    @Override
    protected Object getEntityId(Product entity) {
        return entity.getId();
    }

    @Override
    protected void publishPrimaryEvent(Product formatted) {
        log.debug("Publishing primary product event productId={}", formatted.getId());
        eventPublisher.send(productConfig.kafkaTopic(), formatted.getId(), formatted);
    }

    @Override
    protected void notifySuccess(Product formatted, Map<String, Object> ctx) {
        log.debug("Product processed successfully productId={}", formatted.getId());
        if (productConfig.n8n().enable()) n8nNotificationService.success(true, productConfig.n8n().webhookUrl(), formatted, ctx);
    }

    @Override
    protected void notifyError(Product original, Exception e, Map<String, Object> ctx) {
        log.error("Product processing failed productId={} errorMsg={}", original.getId(), e.getMessage(), e);
        if (productConfig.n8n().enable()) n8nNotificationService.error(true, productConfig.n8n().errorWebhookUrl(), e.getMessage(), ctx);
    }

    @Override
    public PollerMetadata getPoller() {
        return super.getPoller();
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }
}
