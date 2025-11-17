package com.glamaya.glamayawoocommercesync.repository;

import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessorStatusTrackerRepository extends ReactiveMongoRepository<ProcessorStatusTracker, ProcessorType> {

}