package com.glamaya.sync.runner.adapter.store;

import com.glamaya.sync.core.domain.model.ProcessorType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reactive Spring Data MongoDB Repository for ProcessorStatusDocument.
 */
@Repository
public interface ProcessorStatusMongoRepository extends ReactiveMongoRepository<ProcessorStatusDocument, ProcessorType> {
}
