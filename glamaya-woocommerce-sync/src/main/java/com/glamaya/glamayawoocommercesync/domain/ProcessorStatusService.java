package com.glamaya.glamayawoocommercesync.domain;

import java.util.List;
import java.util.function.Function;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

/**
 * A domain service responsible for encapsulating the business rules related to
 * updating and managing the state of a {@link ProcessorStatus}.
 * This service operates purely on domain objects and contains no infrastructure concerns.
 */
public class ProcessorStatusService {

    /**
     * Resets the {@link ProcessorStatus} to its initial state (page 1, use last updated date)
     * when an empty page is fetched.
     *
     * @param status The {@link ProcessorStatus} to reset.
     */
    public void resetAfterEmptyPage(ProcessorStatus status) {
        if (status == null) return;
        status.setPage(1);
        status.setUseLastUpdatedDateInQuery(true);
    }

    /**
     * Advances the {@link ProcessorStatus} after a successful batch fetch.
     * It updates the page number, total count, and the last updated date based on the fetched response.
     *
     * @param status        The {@link ProcessorStatus} to update.
     * @param response      The list of entities fetched in the current batch.
     * @param dateExtractor A function to extract the modified date string from an entity.
     */
    public void advanceAfterBatch(ProcessorStatus status, List<?> response, Function<Object, String> dateExtractor) {
        if (status == null || response == null || response.isEmpty()) {
            return;
        }

        long newCount = status.getCount() + response.size();
        status.setCount(newCount);
        status.setPage(status.getPage() + 1);

        // Update lastUpdatedDate based on the last entity in the response
        String lastDateModified = dateExtractor.apply(response.getLast());
        if (lastDateModified != null) {
            status.setLastUpdatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(lastDateModified));
        }
    }
}
