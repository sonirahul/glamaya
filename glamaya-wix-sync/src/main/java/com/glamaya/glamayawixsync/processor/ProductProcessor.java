package com.glamaya.glamayawixsync.processor;

import com.glamaya.datacontracts.wix.Paging;
import com.glamaya.datacontracts.wix.Product;
import com.glamaya.datacontracts.wix.ProductQuery;
import com.glamaya.datacontracts.wix.ProductQueryRequest;
import com.glamaya.datacontracts.wix.ProductQueryResponse;
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
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductProcessor implements GlamWixProcessor<List<Product>, ProductQueryRequest, ProductQueryResponse> {

    private final WebClient webClient;
    private final KafkaProducer<Product> producer;
    @Getter
    private final PollerMetadata poller;
    private final ProcessorStatusTrackerRepository repository;

    @Value("${application.wix.entities.products.query-url}")
    private final String queryProductsUrl;
    @Value("${application.wix.entities.products.enable}")
    private final boolean enable;
    @Value("${application.wix.entities.products.fetch-limit}")
    private final long fetchLimit;
    @Value("${application.wix.entities.products.reset-on-startup: false}")
    private boolean resetOnStartup;
    @Value("${application.kafka.topic.product-events}")
    private final String productEventsTopic;
    @Value("${application.wix.entities.products.fetch-duration-in-millis.active-mode}")
    private int fetchDurationInMillisActiveMode;
    @Value("${application.wix.entities.products.fetch-duration-in-millis.passive-mode}")
    private int fetchDurationInMillisPassiveMode;

    @Override
    public MessageSource<List<Product>> receive() {
        return () -> {

            if (!enable) {
                log.info("Product sync is disabled");
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode * 60);
                return null;
            }

            ProcessorStatusTracker statusTracker = getOrCreateStatusTracker(resetOnStartup, fetchLimit);
            resetOnStartup = false; // Reset only once on startup

            var request = buildProductQueryRequest(statusTracker);

            var response = fetchEntities(webClient, queryProductsUrl, request, new ParameterizedTypeReference<>() {});

            var isResponseNullOrEmpty = response == null || response.getProducts() == null || response.getProducts().isEmpty();
            if (isResponseNullOrEmpty) {
                log.info("No new wix products found, resetting status tracker and switching to passive mode");
                resetStatusTracker(statusTracker);
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode);
            } else {
                log.info("Fetched {} wix products", statusTracker.getCount() + response.getProducts().size());
                updateStatusTracker(statusTracker, response.getProducts(), null, c -> ((Product) c).getLastUpdated());
                modifyPollerDuration(poller, fetchDurationInMillisActiveMode);
            }
            repository.save(statusTracker).block();
            return isResponseNullOrEmpty ? null : MessageBuilder.withPayload(response.getProducts()).build();
        };
    }

    private ProductQueryRequest buildProductQueryRequest(ProcessorStatusTracker statusTracker) {
        var productQuery = ProductQuery.builder()
                .withPaging(Paging.builder().withLimit(fetchLimit).withOffset(statusTracker.getOffset()).build())
                .withSort("[{\"lastUpdated\": \"asc\"}]").build();

        if (statusTracker.getOffset() == 0 && statusTracker.getLastUpdatedDate() != null) {
            // filter products that were updated after the last updated date
            productQuery.setFilter("{\"lastUpdated\":{\"$gt\":\"" + statusTracker.getLastUpdatedDate() + "\"}}");
        }

        return ProductQueryRequest.builder()
                .withQuery(productQuery)
                .withIncludeHiddenProducts(true)
                .withIncludeMerchantSpecificData(true)
                .withIncludeVariants(true)
                .build();
    }

    @Override
    public ProcessorStatusTrackerRepository getStatusTrackerRepository() {
        return repository;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WIX_PRODUCT;
    }

    @Override
    public Object handle(List<Product> payload, MessageHeaders headers) {
        payload.forEach(product -> producer.send(productEventsTopic, product.getId(), product));
        return null;
    }
}