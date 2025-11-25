package com.glamaya.glamayawoocommercesync.adapter.out.persistence;

import com.glamaya.glamayawoocommercesync.adapter.out.persistence.entity.ProcessorStatusDocument;
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for {@link ProcessorStatusDocument}.
 * This repository provides standard CRUD operations for persisting and retrieving
 * processor status information from a MongoDB database.
 * It acts as a driven adapter for the persistence layer.
 */
@Repository
public interface ProcessorStatusDocumentRepository extends ReactiveMongoRepository<ProcessorStatusDocument, ProcessorType> {
}
