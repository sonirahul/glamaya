package com.glamaya.sync.platform.woocommerce.adapter.util;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Pagination/status helpers reused across WooCommerce processors.
 */
public final class WooPagination {
    private WooPagination() {}

    public static <E> void updateStatusAfterPage(ProcessorStatus status, List<E> pageItems, APIConfig config,
                                                 Function<E, Instant> lastModifiedExtractor) {
        if (pageItems == null || pageItems.isEmpty()) {
            status.setMoreDataAvailable(false);
            status.setNextPage(config.getInitPage());
            status.setUseLastDateModifiedInQuery(true);
        } else {
            status.setTotalItemsSynced(status.getTotalItemsSynced() + pageItems.size());
            E lastItem = pageItems.getLast();
            status.setLastDateModified(lastModifiedExtractor.apply(lastItem));

            if (pageItems.size() < config.getPageSize()) {
                status.setMoreDataAvailable(false);
                status.setNextPage(config.getInitPage());
                status.setUseLastDateModifiedInQuery(true);
            } else {
                status.setNextPage(status.getNextPage() + 1);
                status.setUseLastDateModifiedInQuery(false);
            }
        }
    }
}

