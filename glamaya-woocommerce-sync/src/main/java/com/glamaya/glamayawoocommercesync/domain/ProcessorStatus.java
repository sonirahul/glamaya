package com.glamaya.glamayawoocommercesync.domain;

import java.time.Instant;

public class ProcessorStatus {
    private ProcessorType processorType;
    private int page;
    private long pageSize;
    private long count;
    private boolean useLastUpdatedDateInQuery;
    private Instant lastUpdatedDate;

    // Constructor
    public ProcessorStatus(ProcessorType processorType, int page, long pageSize, long count, boolean useLastUpdatedDateInQuery, Instant lastUpdatedDate) {
        this.processorType = processorType;
        this.page = page;
        this.pageSize = pageSize;
        this.count = count;
        this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    // Getters
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

    // Setters
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

    // Builder pattern (manual implementation as Lombok is problematic)
    public static ProcessorStatusBuilder builder() {
        return new ProcessorStatusBuilder();
    }

    public static class ProcessorStatusBuilder {
        private ProcessorType processorType;
        private int page;
        private long pageSize;
        private long count;
        private boolean useLastUpdatedDateInQuery;
        private Instant lastUpdatedDate;

        ProcessorStatusBuilder() {}

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

        public ProcessorStatus build() {
            return new ProcessorStatus(processorType, page, pageSize, count, useLastUpdatedDateInQuery, lastUpdatedDate);
        }
    }
}
