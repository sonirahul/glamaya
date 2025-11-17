package com.glamaya.glamayawixsync.repository;

import com.glamaya.glamayawixsync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawixsync.repository.entity.ProcessorType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessorStatusTrackerRepository extends ReactiveMongoRepository<ProcessorStatusTracker, ProcessorType> {

}