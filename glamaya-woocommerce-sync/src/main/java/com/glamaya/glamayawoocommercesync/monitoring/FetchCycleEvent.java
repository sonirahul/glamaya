package com.glamaya.glamayawoocommercesync.monitoring;

import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;

public record FetchCycleEvent(ProcessorType processorType,
                              int itemCount,
                              boolean empty,
                              boolean error,
                              long durationNanos) {
}
