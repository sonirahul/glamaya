package com.glamaya.glamayawoocommercesync.domain;

import java.time.Instant;

/**
 * Represents the current status of a WooCommerce processor within the domain.
 * This is a pure domain entity, free from any persistence-specific annotations or framework dependencies.
 * It holds the essential state required for tracking the progress of data synchronization.
 */
public class ProcessorStatus {
    private ProcessorType processorType;
    private int page;
    private long pageSize;
    private long count;
    private boolean useLastUpdatedDateInQuery;
    private Instant lastUpdatedDate;

    /**
     * Constructs a new {@code ProcessorStatus}.
     *
     * @param processorType             The type of the processor.
     * @param page                      The current page number being processed.
     * @param pageSize                  The number of items per page.
     * @param count                     The total count of items processed so far.
     * @param useLastUpdatedDateInQuery Whether to use the last updated date in the next query.
     * @param lastUpdatedDate           The timestamp of the last updated item.
     */
    public ProcessorStatus(ProcessorType processorType, int page, long pageSize, long count, boolean useLastUpdatedDateInQuery, Instant lastUpdatedDate) {
        this.processorType = processorType;
        this.page = page;
        this.pageSize = pageSize;
        this.count = count;
        this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    // --- Getters ---

    public ProcessorType getProcessorType() {
        return processorType;
    }

    public int getPage() {
        return page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getCount() {
        return count;
    }

    public boolean isUseLastUpdatedDateInQuery() {
        return useLastUpdatedDateInQuery;
    }

    public Instant getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    // --- Setters ---

    public void setProcessorType(ProcessorType processorType) {
        this.processorType = processorType;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setUseLastUpdatedDateInQuery(boolean useLastUpdatedDateInQuery) {
        this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
    }

    public void setLastUpdatedDate(Instant lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    // --- Builder Pattern (Manual Implementation) ---

    /**
     * Creates a new builder for {@link ProcessorStatus}.
     *
     * @return A {@link ProcessorStatusBuilder} instance.
     */
    public static ProcessorStatusBuilder builder() {
        return new ProcessorStatusBuilder();
    }

    /**
     * Builder class for {@link ProcessorStatus}.
     */
    public static class ProcessorStatusBuilder {
        private ProcessorType processorType;
        private int page;
        private long pageSize;
        private long count;
        private boolean useLastUpdatedDateInQuery;
        private Instant lastUpdatedDate;

        ProcessorStatusBuilder() {
        }

        public ProcessorStatusBuilder processorType(ProcessorType processorType) {
            this.processorType = processorType;
            return this;
        }

        public ProcessorStatusBuilder page(int page) {
            this.page = page;
            return this;
        }

        public ProcessorStatusBuilder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public ProcessorStatusBuilder count(long count) {
            this.count = count;
            return this;
        }

        public ProcessorStatusBuilder useLastUpdatedDateInQuery(boolean useLastUpdatedDateInQuery) {
            this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
            return this;
        }

        public ProcessorStatusBuilder lastUpdatedDate(Instant lastUpdatedDate) {
            this.lastUpdatedDate = lastUpdatedDate;
            return this;
        }

        /**
         * Builds a {@link ProcessorStatus} instance from the builder's properties.
         *
         * @return A new {@link ProcessorStatus} object.
         */
        public ProcessorStatus build() {
            return new ProcessorStatus(processorType, page, pageSize, count, useLastUpdatedDateInQuery, lastUpdatedDate);
        }

        @Override
        public String toString() {
            return "ProcessorStatus.ProcessorStatusBuilder(processorType=" + this.processorType + ", page=" + this.page + ", pageSize=" + this.pageSize + ", count=" + this.count + ", useLastUpdatedDateInQuery=" + this.useLastUpdatedDateInQuery + ", lastUpdatedDate=" + this.lastUpdatedDate + ")";
        }
    }
}
