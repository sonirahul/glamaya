package com.glamaya.glamayawixsync.repository.entity;

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
    // page and count are the same as offset in wix API
    private Long offset;
    // page size is fetch limit in wix API
    private Long fetchLimit;
    // cursor is used for pagination in wix API for orders
    private String cursor;
    private Long count;
    private Instant lastUpdatedDate;
}