package com.glamaya.glamayawoocommercesync.monitoring;

import com.glamaya.glamayawoocommercesync.domain.ProcessorType; // Updated import

public record FetchCycleEvent(ProcessorType processorType,
                              int itemCount,
                              boolean empty,
                              boolean error,
                              long durationNanos) {
}
