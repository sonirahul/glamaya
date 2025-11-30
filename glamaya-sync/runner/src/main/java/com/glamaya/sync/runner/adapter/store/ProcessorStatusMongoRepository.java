package com.glamaya.sync.runner.adapter.store;

import com.glamaya.sync.core.domain.model.ProcessorType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessorStatusMongoRepository extends MongoRepository<ProcessorStatusDocument, ProcessorType> {}
