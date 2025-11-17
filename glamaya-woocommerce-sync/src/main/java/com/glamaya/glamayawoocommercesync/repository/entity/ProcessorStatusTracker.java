package com.glamaya.glamayawoocommercesync.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
@Document(collection = "processor-status-tracker")
public class ProcessorStatusTracker {

    @Id
    private ProcessorType processorType;
    private long page = 1;
    private long pageSize;
    private long count;
    private Instant lastUpdatedDate;
    private boolean useLastUpdatedDateInQuery = false;
}