package com.glamaya.glamayawoocommercesync.repository.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "processor_status_tracker")
public class ProcessorStatusTracker {
    @Id
    private ProcessorType processorType;
    private int page;
    private long pageSize;
    private long count;
    private boolean useLastUpdatedDateInQuery;
    private Instant lastUpdatedDate;
}

