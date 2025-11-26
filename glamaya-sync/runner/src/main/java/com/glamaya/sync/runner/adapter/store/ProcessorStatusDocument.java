package com.glamaya.sync.runner.adapter.store;

import com.glamaya.sync.core.domain.model.ProcessorType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representation of the ProcessorStatus.
 * This class is used by the runner module for persistence.
 */
@Data
@Document(collection = "processorStatuses")
public class ProcessorStatusDocument {

    @Id
    private ProcessorType processorType; // Using ProcessorType as the ID for uniqueness
    private Instant lastSuccessfulRun;
    private String cursor;
    private int currentPage;

    // Default constructor for MongoDB
    public ProcessorStatusDocument() {
    }

    public ProcessorStatusDocument(ProcessorType processorType, Instant lastSuccessfulRun, String cursor, int currentPage) {
        this.processorType = processorType;
        this.lastSuccessfulRun = lastSuccessfulRun;
        this.cursor = cursor;
        this.currentPage = currentPage;
    }
}
