package com.glamaya.glamayawoocommercesync.service.impl;

import com.glamaya.glamayawoocommercesync.port.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StatusTrackerStoreImpl implements StatusTrackerStore {

    private final ProcessorStatusTrackerRepository repository;

    @Override
    public Mono<ProcessorStatusTracker> getOrCreate(ProcessorType type, boolean resetOnStartup, long pageSize) {
        if (resetOnStartup) {
            return newTracker(type, pageSize);
        }
        return repository.findById(type)
                .switchIfEmpty(newTracker(type, pageSize))
                .flatMap(existing -> existing.getPageSize() != pageSize ? newTracker(type, pageSize) : Mono.just(existing));
    }

    @Override
    public Mono<ProcessorStatusTracker> save(ProcessorStatusTracker tracker) {
        return repository.save(tracker);
    }

    private Mono<ProcessorStatusTracker> newTracker(ProcessorType type, long pageSize) {
        return repository.save(ProcessorStatusTracker.builder()
                .processorType(type)
                .page(1)
                .pageSize(pageSize)
                .count(0)
                .useLastUpdatedDateInQuery(false)
                .build());
    }
}

