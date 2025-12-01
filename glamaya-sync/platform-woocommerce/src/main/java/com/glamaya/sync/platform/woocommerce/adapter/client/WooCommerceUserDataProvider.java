package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.platform.woocommerce.adapter.client.descriptor.UserDescriptor;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Implementation of DataProvider for fetching WooCommerce Users.
 * This class orchestrates fetching a page and updating the status based on the result.
 */
@Slf4j
@Component
public class WooCommerceUserDataProvider implements DataProvider<User> {

    private final WooCommerceApiService<User> apiService;
    private final UserDescriptor userDescriptor;

    public WooCommerceUserDataProvider(WooCommerceApiService<User> apiService, UserDescriptor userDescriptor) {
        this.apiService = apiService;
        this.userDescriptor = userDescriptor;
    }

    @Override
    public Flux<User> fetchData(SyncContext<?> context) {
        var config = (APIConfig) context.configuration().get();
        var status = context.status();

        return apiService.fetchPage(userDescriptor, status, config)
                .collectList()
                .doOnNext(pageItems -> WooCommerceApiService.updateStatusAfterPage(status, pageItems, config,
                        userDescriptor.getLastModifiedExtractor()))
                .flatMapMany(Flux::fromIterable);
    }
}
