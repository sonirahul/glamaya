package com.glamaya.glamayawoocommercesync.port;

import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import reactor.core.publisher.Mono;

public interface StatusTrackerStore {
    Mono<ProcessorStatusTracker> getOrCreate(ProcessorType type, boolean resetOnStartup, long pageSize);

    Mono<ProcessorStatusTracker> save(ProcessorStatusTracker tracker);
}

