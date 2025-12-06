package com.glamaya.sync.core.application.usecase;

import com.glamaya.sync.core.domain.model.ProcessorType;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Inbound port for triggering synchronization processes for a specific platform entity.
 * This interface defines how the core application logic can be driven by external actors
 * (e.g., a scheduler, a REST controller).
 */
public interface SyncOrchestrator {

    Mono<Void> syncSequential(ProcessorType processorType);

    Mono<Void> syncParallel(int maxConcurrency);

    Mono<Void> syncPlatformParallel(List<ProcessorType> types, int maxConcurrency);
}
