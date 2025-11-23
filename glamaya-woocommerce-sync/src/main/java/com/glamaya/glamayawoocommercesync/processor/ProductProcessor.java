package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.woocommerce.Product;
import com.glamaya.datacontracts.woocommerce.ProductOrderBy;
import com.glamaya.datacontracts.woocommerce.ProductSearchRequest;
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
public class ProductProcessor extends AbstractWooProcessor<Product> {

    private final EventPublisher eventPublisher;
    private final N8nNotificationService n8nNotificationService;

    @Value("${application.kafka.topic.product-events}")
    private String productEventsTopic;
    @Value("${application.woocommerce.entities.products.n8n.webhook-url}")
    private String n8nWebhookUrl;
    @Value("${application.woocommerce.entities.products.n8n.error-webhook-url}")
    private String n8nErrorWebhookUrl;
    @Value("${application.woocommerce.entities.products.n8n.enable}")
    private boolean n8nEnable;

    public ProductProcessor(WebClient woocommerceWebClient,
                            ObjectMapper objectMapper,
                            PollerMetadata poller,
                            @Value("${application.woocommerce.entities.products.page-size}") long pageSize,
                            @Value("${application.woocommerce.entities.products.reset-on-startup}") boolean resetOnStartup,
                            @Value("${application.woocommerce.entities.products.fetch-duration-in-millis.active-mode}") int active,
                            @Value("${application.woocommerce.entities.products.fetch-duration-in-millis.passive-mode}") int passive,
                            @Value("${application.woocommerce.entities.products.query-url}") String queryUrl,
                            @Value("${application.woocommerce.entities.products.enable}") boolean enable,
                            @Value("${application.processing.concurrency:4}") int processingConcurrency,
                            OAuth1Service oAuth1Service,
                            StatusTrackerStore statusTrackerStore,
                            EventPublisher eventPublisher,
                            N8nNotificationService n8nNotificationService,
                            ApplicationEventPublisher eventPublisherPublisher) {
        super(woocommerceWebClient, objectMapper, poller, pageSize, resetOnStartup, active, passive, queryUrl, enable, processingConcurrency, oAuth1Service, statusTrackerStore, eventPublisherPublisher);
        this.eventPublisher = eventPublisher;
        this.n8nNotificationService = n8nNotificationService;
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
                .withPage(Long.valueOf(tracker.getPage()))
                .withPerPage(Long.valueOf(pageSize));
        if (tracker.isUseLastUpdatedDateInQuery() && tracker.getLastUpdatedDate() != null) {
            b.withModifiedAfter(tracker.getLastUpdatedDate());
        }
        return b.build();
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
        eventPublisher.send(productEventsTopic, formatted.getId(), formatted);
    }

    @Override
    protected void notifySuccess(Product formatted, Map<String, Object> ctx) {
        if (n8nEnable) n8nNotificationService.success(true, n8nWebhookUrl, formatted, ctx);
    }

    @Override
    protected void notifyError(Product original, Exception e, Map<String, Object> ctx) {
        if (n8nEnable) n8nNotificationService.error(true, n8nErrorWebhookUrl, e.getMessage(), ctx);
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
