package com.glamaya.glamayawoocommercesync.monitoring;

import com.glamaya.glamayawoocommercesync.domain.ProcessorType;

/**
 * A record representing an event that signals the completion of a single fetch cycle
 * for a WooCommerce processor. This event is used for monitoring and metrics collection.
 *
 * @param processorType The {@link ProcessorType} that completed a fetch cycle.
 * @param itemCount     The number of items fetched in this cycle.
 * @param empty         {@code true} if no items were fetched, {@code false} otherwise.
 * @param error         {@code true} if an error occurred during the fetch cycle, {@code false} otherwise.
 * @param durationNanos The duration of the fetch cycle in nanoseconds.
 */
public record FetchCycleEvent(ProcessorType processorType,
                              int itemCount,
                              boolean empty,
                              boolean error,
                              long durationNanos) {
}
