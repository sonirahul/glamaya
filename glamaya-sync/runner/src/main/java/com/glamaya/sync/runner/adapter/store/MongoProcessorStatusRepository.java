package com.glamaya.sync.runner.adapter.store;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.StatusStorePort;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Reactive adapter implementation of the StatusStorePort using Spring Data MongoDB.
 * This class translates between the core's ProcessorStatus domain model
 * and the MongoDB-specific ProcessorStatusDocument.
 */
@Repository
public class MongoProcessorStatusRepository implements StatusStorePort {

    private final ProcessorStatusMongoRepository mongoRepository;
    private final ProcessorStatusMapper processorStatusMapper;

    public MongoProcessorStatusRepository(ProcessorStatusMongoRepository mongoRepository, ProcessorStatusMapper processorStatusMapper) {
        this.mongoRepository = mongoRepository;
        this.processorStatusMapper = processorStatusMapper;
    }

    @Override
    public Mono<ProcessorStatus> findStatus(ProcessorType processorType) {
        return mongoRepository.findById(processorType)
                .map(processorStatusMapper::toDomain);
    }

    @Override
    public Mono<Void> saveStatus(ProcessorStatus status) {
        return mongoRepository.save(processorStatusMapper.toDocument(status)).then();
    }
}
