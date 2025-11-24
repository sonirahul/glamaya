package com.glamaya.glamayawoocommercesync.adapter.out.persistence;

import com.glamaya.glamayawoocommercesync.adapter.out.persistence.entity.ProcessorStatusDocument; // Updated import
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessorStatusDocumentRepository extends ReactiveMongoRepository<ProcessorStatusDocument, ProcessorType> { // Renamed interface

}
