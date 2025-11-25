package com.glamaya.glamayawoocommercesync.adapter.out.persistence.entity;

import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents the persistence-specific entity for storing {@link com.glamaya.glamayawoocommercesync.domain.ProcessorStatus}
 * in a MongoDB database. This class includes MongoDB-specific annotations.
 * It acts as a data transfer object between the persistence adapter and the database.
 */
@Data
@Document(collection = "processor_status_tracker")
public class ProcessorStatusDocument {
    @Id
    private ProcessorType processorType;
    private int page;
    private long pageSize;
    private long count;
    private boolean useLastUpdatedDateInQuery;
    private Instant lastUpdatedDate;

    /**
     * Constructs a new {@code ProcessorStatusDocument}.
     *
     * @param processorType             The type of the processor.
     * @param page                      The current page number.
     * @param pageSize                  The configured page size.
     * @param count                     The total count of items processed.
     * @param useLastUpdatedDateInQuery Whether to use the last updated date in the query.
     * @param lastUpdatedDate           The timestamp of the last updated item.
     */
    public ProcessorStatusDocument(ProcessorType processorType, int page, long pageSize, long count, boolean useLastUpdatedDateInQuery, Instant lastUpdatedDate) {
        this.processorType = processorType;
        this.page = page;
        this.pageSize = pageSize;
        this.count = count;
        this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    // --- Getters and Setters (Manually added due to Lombok processing issues) ---

    public ProcessorType getProcessorType() {
        return processorType;
    }

    public void setProcessorType(ProcessorType processorType) {
        this.processorType = processorType;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public boolean isUseLastUpdatedDateInQuery() {
        return useLastUpdatedDateInQuery;
    }

    public void setUseLastUpdatedDateInQuery(boolean useLastUpdatedDateInQuery) {
        this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
    }

    public Instant getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Instant lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    // --- Builder Pattern (Manual Implementation) ---

    /**
     * Creates a new builder for {@link ProcessorStatusDocument}.
     *
     * @return A {@link ProcessorStatusDocumentBuilder} instance.
     */
    public static ProcessorStatusDocumentBuilder builder() {
        return new ProcessorStatusDocumentBuilder();
    }

    /**
     * Builder class for {@link ProcessorStatusDocument}.
     */
    public static class ProcessorStatusDocumentBuilder {
        private ProcessorType processorType;
        private int page;
        private long pageSize;
        private long count;
        private boolean useLastUpdatedDateInQuery;
        private Instant lastUpdatedDate;

        ProcessorStatusDocumentBuilder() {
        }

        public ProcessorStatusDocumentBuilder processorType(ProcessorType processorType) {
            this.processorType = processorType;
            return this;
        }

        public ProcessorStatusDocumentBuilder page(int page) {
            this.page = page;
            return this;
        }

        public ProcessorStatusDocumentBuilder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public ProcessorStatusDocumentBuilder count(long count) {
            this.count = count;
            return this;
        }

        public ProcessorStatusDocumentBuilder useLastUpdatedDateInQuery(boolean useLastUpdatedDateInQuery) {
            this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
            return this;
        }

        public ProcessorStatusDocumentBuilder lastUpdatedDate(Instant lastUpdatedDate) {
            this.lastUpdatedDate = lastUpdatedDate;
            return this;
        }

        /**
         * Builds a {@link ProcessorStatusDocument} instance from the builder's properties.
         *
         * @return A new {@link ProcessorStatusDocument} object.
         */
        public ProcessorStatusDocument build() {
            return new ProcessorStatusDocument(processorType, page, pageSize, count, useLastUpdatedDateInQuery, lastUpdatedDate);
        }

        @Override
        public String toString() {
            return "ProcessorStatusDocument.ProcessorStatusDocumentBuilder(processorType=" + this.processorType + ", page=" + this.page + ", pageSize=" + this.pageSize + ", count=" + this.count + ", useLastUpdatedDateInQuery=" + this.useLastUpdatedDateInQuery + ", lastUpdatedDate=" + this.lastUpdatedDate + ")";
        }
    }
}
