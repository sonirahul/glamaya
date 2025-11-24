package com.glamaya.glamayawoocommercesync.adapter.out.persistence.entity;

import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "processor_status_tracker")
public class ProcessorStatusDocument { // Renamed class
    @Id
    private ProcessorType processorType;
    private int page;
    private long pageSize;
    private long count;
    private boolean useLastUpdatedDateInQuery;
    private Instant lastUpdatedDate;

    // Manually added constructor for builder
    public ProcessorStatusDocument(ProcessorType processorType, int page, long pageSize, long count, boolean useLastUpdatedDateInQuery, Instant lastUpdatedDate) {
        this.processorType = processorType;
        this.page = page;
        this.pageSize = pageSize;
        this.count = count;
        this.useLastUpdatedDateInQuery = useLastUpdatedDateInQuery;
        this.lastUpdatedDate = lastUpdatedDate;
    }

    // Manually added builder method
    public static ProcessorStatusDocumentBuilder builder() {
        return new ProcessorStatusDocumentBuilder();
    }

    // Manually added getters and setters (Lombok @Data replacement)
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

    // Manually added Builder class (Lombok @Builder replacement)
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

        public ProcessorStatusDocument build() {
            return new ProcessorStatusDocument(processorType, page, pageSize, count, useLastUpdatedDateInQuery, lastUpdatedDate);
        }

        public String toString() {
            return "ProcessorStatusDocument.ProcessorStatusDocumentBuilder(processorType=" + this.processorType + ", page=" + this.page + ", pageSize=" + this.pageSize + ", count=" + this.count + ", useLastUpdatedDateInQuery=" + this.useLastUpdatedDateInQuery + ", lastUpdatedDate=" + this.lastUpdatedDate + ")";
        }
    }
}
