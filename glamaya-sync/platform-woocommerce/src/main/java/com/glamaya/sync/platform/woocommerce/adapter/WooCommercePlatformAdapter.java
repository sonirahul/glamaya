package com.glamaya.sync.platform.woocommerce.adapter;

import com.glamaya.sync.core.application.usecase.SyncPlatformUseCase;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.PlatformAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WooCommerce-specific implementation of the PlatformAdapter outbound port.
 * This adapter orchestrates the synchronization of various WooCommerce entities
 * by delegating to the core SyncPlatformUseCase.
 */
@Slf4j
@Component
public class WooCommercePlatformAdapter implements PlatformAdapter {

    private final SyncPlatformUseCase syncPlatformUseCase;

    public WooCommercePlatformAdapter(SyncPlatformUseCase syncPlatformUseCase) {
        this.syncPlatformUseCase = syncPlatformUseCase;
    }

    @Override
    public String getPlatformName() {
        return "woocommerce";
    }

    @Override
    public Mono<Void> sync() {
        log.info("Initiating full synchronization for WooCommerce platform.");
        // Sequentially trigger sync for each processor type.
        // Use concatMap to ensure they run one after the other, not in parallel.
        return Flux.just(ProcessorType.WOOCOMMERCE_ORDER, ProcessorType.WOOCOMMERCE_USER)
                .concatMap(syncPlatformUseCase::sync)
                .then()
                .doOnSuccess(v -> log.info("Completed full synchronization for WooCommerce platform."));
    }
}
