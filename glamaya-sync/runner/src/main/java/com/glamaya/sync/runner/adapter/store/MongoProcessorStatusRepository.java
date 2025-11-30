package com.glamaya.sync.runner.adapter.store;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.ProcessorType;
import com.glamaya.sync.core.domain.port.out.StatusStorePort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB Repository for ProcessorStatusDocument.
 * This interface extends MongoRepository to provide CRUD operations for ProcessorStatusDocument.
 */
interface ProcessorStatusMongoRepository extends MongoRepository<ProcessorStatusDocument, ProcessorType> {
}

/**
 * Adapter implementation of the StatusStorePort using Spring Data MongoDB.
 * This class translates between the core's ProcessorStatus domain model
 * and the MongoDB-specific ProcessorStatusDocument.
 */
@Repository
public class MongoProcessorStatusRepository implements StatusStorePort {

    private final ProcessorStatusMongoRepository mongoRepository;

    public MongoProcessorStatusRepository(ProcessorStatusMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Optional<ProcessorStatus> findStatus(ProcessorType processorType) {
        return mongoRepository.findById(processorType)
                .map(this::toDomain);
    }

    @Override
    public void saveStatus(ProcessorStatus status) {
        mongoRepository.save(toDocument(status));
    }

    private ProcessorStatus toDomain(ProcessorStatusDocument document) {
        ProcessorStatus domain = new ProcessorStatus(document.getProcessorType());
        domain.setLastSuccessfulRun(document.getLastSuccessfulRun());
        domain.setCursor(document.getCursor());
        domain.setCurrentPage(document.getCurrentPage());
        domain.setPageSize(document.getPageSize());
        domain.setTotalItemsSynced(document.getTotalItemsSynced());
        return domain;
    }

    private ProcessorStatusDocument toDocument(ProcessorStatus domain) {
        return new ProcessorStatusDocument(
                domain.getProcessorType(),
                domain.getLastSuccessfulRun(),
                domain.getCursor(),
                domain.getCurrentPage(),
                domain.getPageSize(),
                domain.getTotalItemsSynced()
        );
    }
}
