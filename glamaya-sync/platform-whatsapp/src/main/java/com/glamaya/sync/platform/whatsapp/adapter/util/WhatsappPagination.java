package com.glamaya.sync.platform.whatsapp.adapter.util;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.platform.whatsapp.config.APIConfig;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Pagination/status helpers reused across WhatsApp processors.
 */
public final class WhatsappPagination {
    private WhatsappPagination() {}

    /**
     * Updates the processor status after fetching a page of WhatsApp entities.
     *
     * @param status               The processor status to update.
     * @param pageItems            The list of items fetched in the current page.
     * @param config               The API configuration.
     * @param lastModifiedExtractor Function to extract last modified date from an entity.
     */
    public static <E> void updateStatusAfterPage(ProcessorStatus status, List<E> pageItems, APIConfig config,
                                                 Function<E, Instant> lastModifiedExtractor) {
        if (pageItems == null || pageItems.isEmpty()) {
            status.setMoreDataAvailable(false);
        } else {
            status.setTotalItemsSynced(status.getTotalItemsSynced() + pageItems.size());
            E lastItem = pageItems.getLast();
            status.setLastDateModified(lastModifiedExtractor.apply(lastItem));

            if (pageItems.size() < config.getPageSize()) {
                status.setMoreDataAvailable(false);
            } else {
                status.setNextPage(status.getNextPage() + 1);
            }
        }
    }
}
