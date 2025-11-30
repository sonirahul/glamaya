package com.glamaya.sync.core.domain.port.out;

import com.glamaya.sync.core.domain.model.SyncContext;
import java.util.List;

/**
 * An outbound port defining the contract for fetching raw data from an external platform.
 *
 * @param <T> The type of the platform-specific data transfer object (DTO) that is returned by the API.
 */
public interface DataProvider<T> {

    /**
     * Fetches a batch of data from the external platform's API.
     *
     * @param context The SyncContext containing the current status (like last run date or cursor)
     *                and configuration needed to make the API call.
     * @return A list of raw, platform-specific DTOs. Returns an empty list if no new data is found.
     */
    List<T> fetchData(SyncContext<?> context);
}
