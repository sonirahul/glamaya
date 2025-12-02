package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.woocommerce.Order;
import com.glamaya.datacontracts.woocommerce.OrderOrderBy;
import com.glamaya.datacontracts.woocommerce.OrderSearchRequest;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.client.descriptor.OrderDescriptor;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Implementation of DataProvider for fetching WooCommerce Orders.
 * This class orchestrates fetching a page and updating the status based on the result.
 */
@Slf4j
@Component
public class WooCommerceOrderDataProvider implements DataProvider<Order> {

    private final WooCommerceApiService<Order> apiService;
    private final OrderDescriptor orderDescriptor;
    private final ObjectMapper objectMapper;

    public WooCommerceOrderDataProvider(WooCommerceApiService<Order> apiService, OrderDescriptor orderDescriptor, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.orderDescriptor = orderDescriptor;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<Order> fetchData(SyncContext<?> context) {
        var config = (APIConfig) context.configuration().get();
        var status = context.status();
        var queryParams = buildQueryParams(status, config);

        return apiService.fetchPage(orderDescriptor, queryParams, status, config)
                .collectList()
                .doOnNext(pageItems -> WooCommerceApiService.updateStatusAfterPage(status, pageItems, config,
                        orderDescriptor.getLastModifiedExtractor()))
                .flatMapMany(Flux::fromIterable);
    }

    private Map<String, String> buildQueryParams(ProcessorStatus statusTracker, APIConfig config) {

        var builder = OrderSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(OrderOrderBy.date_modified)
                .withOrder(SortOrder.asc)
                .withPage(Long.valueOf(statusTracker.getNextPage()))
                .withPerPage(Long.valueOf(config.getPageSize()));

        if (statusTracker.isUseLastDateModifiedInQuery() && statusTracker.getLastDateModified() != null) {
            builder.withModifiedAfter(statusTracker.getLastDateModified());
        }

        return objectMapper.convertValue(builder.build(), new TypeReference<>() {
        });
    }
}
