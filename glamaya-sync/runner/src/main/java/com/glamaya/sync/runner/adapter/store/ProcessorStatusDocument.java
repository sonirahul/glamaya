package com.glamaya.sync.runner.adapter.store;

import com.glamaya.sync.core.domain.model.ProcessorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representation of the ProcessorStatus.
 * This class is used by the runner module for persistence.
 */
@Data
@Document(collection = "processor_status")
@NoArgsConstructor
@AllArgsConstructor
public class ProcessorStatusDocument {

    @Id
    private ProcessorType processorType;
    private Instant lastSuccessfulRun;
    private String cursor;
    private int currentPage;
    private int pageSize;
    private int totalItemsSynced;
}
