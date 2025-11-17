package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.woocommerce.Product;
import com.glamaya.datacontracts.woocommerce.ProductOrderBy;
import com.glamaya.datacontracts.woocommerce.ProductSearchRequest;
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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductProcessor implements GlamWoocommerceProcessor<List<Product>> {

    @Qualifier("woocommerceWebClient")
    private final WebClient woocommerceWebClient;
    @Qualifier("n8nWebhookWebClient")
    private final WebClient n8nWebClient;
    private final OAuth1Service oAuth1Service;
    private final KafkaProducer<Product> producer;
    @Getter
    private final PollerMetadata poller;
    private final ObjectMapper objectMapper;
    private final ProcessorStatusTrackerRepository repository;

    @Value("${application.woocommerce.entities.products.query-url}")
    private final String queryProductsUrl;
    @Value("${application.woocommerce.entities.products.n8n.webhook-url}")
    private final String n8nWebhookUrl;
    @Value("${application.woocommerce.entities.products.n8n.error-webhook-url}")
    private final String n8nErrorWebhookUrl;
    @Value("${application.woocommerce.entities.products.n8n.enable}")
    private final boolean n8nEnable;
    @Value("${application.woocommerce.entities.products.enable}")
    private final boolean enable;
    @Value("${application.woocommerce.entities.products.page-size}")
    private final long pageSize;
    @Value("${application.woocommerce.entities.products.reset-on-startup}")
    private boolean resetOnStartup;
    @Value("${application.kafka.topic.product-events}")
    private final String productEventsTopic;
    @Value("${application.woocommerce.entities.products.fetch-duration-in-millis.active-mode}")
    private int fetchDurationInMillisActiveMode;
    @Value("${application.woocommerce.entities.products.fetch-duration-in-millis.passive-mode}")
    private int fetchDurationInMillisPassiveMode;

    public MessageSource<List<Product>> receive() {
        return () -> {
            if (!enable) {
                log.info("Product sync is disabled");
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode * 60);
                return null;
            }

            ProcessorStatusTracker statusTracker = getOrCreateStatusTracker(resetOnStartup, pageSize);
            resetOnStartup = false; // Reset only once on startup

            ProductSearchRequest productSearchRequest = buildProductSearchRequest(statusTracker);

            Map<String, String> queryParamMap = objectMapper.convertValue(productSearchRequest, new TypeReference<>() {
            });
            String oauthHeader = oAuth1Service.generateOAuth1Header(queryProductsUrl, queryParamMap);

            List<Product> response = fetchEntities(woocommerceWebClient, queryProductsUrl, queryParamMap, oauthHeader,
                    new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.isEmpty()) {
                log.info("No new woocommerce Products found, resetting status tracker and switching to passive mode");
                resetStatusTracker(statusTracker);
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode);
            } else {
                log.info("Fetched {} woocommerce Products", statusTracker.getCount() + response.size());
                updateStatusTracker(statusTracker, response, o -> ((Product) o).getDateModifiedGmt());
                modifyPollerDuration(poller, fetchDurationInMillisActiveMode);
            }
            repository.save(statusTracker).block();

            return response == null ? null : MessageBuilder.withPayload(response).build();
        };
    }

    private ProductSearchRequest buildProductSearchRequest(ProcessorStatusTracker statusTracker) {
        var builder = ProductSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(ProductOrderBy.date_modified)
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
        return ProcessorType.WOO_PRODUCT;
    }

    @Override
    public Consumer<SourcePollingChannelAdapterSpec> poll() {
        return e -> e.poller(getPoller());
    }

    @Override
    public Object handle(List<Product> payload, MessageHeaders headers) {
        payload.forEach(product -> {
            try {
                publishData(n8nWebClient, n8nWebhookUrl, product, n8nEnable);
                producer.send(productEventsTopic, product.getId().toString(), product);
            } catch (Exception e) {
                log.error("Error processing product: {}", product.getId(), e);
                publishData(n8nWebClient, n8nErrorWebhookUrl, e.getMessage(), n8nEnable);
            }
        });
        return null;
    }
}