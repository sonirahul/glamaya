package com.glamaya.glamayawoocommercesync.port.out;

import com.glamaya.glamayawoocommercesync.domain.ProcessorStatus; // Updated to domain entity
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import reactor.core.publisher.Mono;

public interface StatusTrackerStore {
    Mono<ProcessorStatus> getOrCreate(ProcessorType type, boolean resetOnStartup, long pageSize); // Updated return type and parameter type

    Mono<ProcessorStatus> save(ProcessorStatus tracker); // Updated parameter type
}
