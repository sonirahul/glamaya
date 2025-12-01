package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.datacontracts.woocommerce.UserOrderBy;
import com.glamaya.datacontracts.woocommerce.UserSearchRequest;
import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.client.descriptor.UserDescriptor;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Implementation of DataProvider for fetching WooCommerce Users.
 * This class orchestrates fetching a page and updating the status based on the result.
 */
@Slf4j
@Component
public class WooCommerceUserDataProvider implements DataProvider<User> {

    private final WooCommerceApiService<User> apiService;
    private final UserDescriptor userDescriptor;
    private final ObjectMapper objectMapper;

    public WooCommerceUserDataProvider(WooCommerceApiService<User> apiService, UserDescriptor userDescriptor, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.userDescriptor = userDescriptor;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<User> fetchData(SyncContext<?> context) {
        var config = (APIConfig) context.configuration().get();
        var status = context.status();
        var queryParams = buildQueryParams(status, config);

        return apiService.fetchPage(userDescriptor, queryParams, status, config)
                .collectList()
                .doOnNext(pageItems -> WooCommerceApiService.updateStatusAfterPage(status, pageItems, config,
                        userDescriptor.getLastModifiedExtractor()))
                .flatMapMany(Flux::fromIterable);
    }

    private Map<String, String> buildQueryParams(ProcessorStatus statusTracker, APIConfig config) {

        var builder = UserSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(UserOrderBy.id)
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
