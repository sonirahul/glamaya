package com.glamaya.glamayawoocommercesync.adapter.out.persistence;

import com.glamaya.glamayawoocommercesync.port.out.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.adapter.out.persistence.ProcessorStatusDocumentRepository; // Updated import
import com.glamaya.glamayawoocommercesync.adapter.out.persistence.entity.ProcessorStatusDocument; // Updated import
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatus;
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StatusTrackerPersistenceAdapter implements StatusTrackerStore {

    private final ProcessorStatusDocumentRepository repository; // Updated type

    @Override
    public Mono<ProcessorStatus> getOrCreate(ProcessorType type, boolean resetOnStartup, long pageSize) {
        if (resetOnStartup) {
            return newTracker(type, pageSize).map(this::toDomain);
        }
        return repository.findById(type)
                .switchIfEmpty(newTracker(type, pageSize))
                .flatMap(existing -> existing.getPageSize() != pageSize ? newTracker(type, pageSize) : Mono.just(existing))
                .map(this::toDomain);
    }

    @Override
    public Mono<ProcessorStatus> save(ProcessorStatus tracker) {
        return repository.save(toDocument(tracker)).map(this::toDomain); // Updated method call
    }

    private Mono<ProcessorStatusDocument> newTracker(ProcessorType type, long pageSize) {
        return repository.save(ProcessorStatusDocument.builder()
                .processorType(type)
                .page(1)
                .pageSize(pageSize)
                .count(0)
                .useLastUpdatedDateInQuery(false)
                .build());
    }

    private ProcessorStatus toDomain(ProcessorStatusDocument document) { // Updated parameter type
        return new ProcessorStatus(
                document.getProcessorType(),
                document.getPage(),
                document.getPageSize(),
                document.getCount(),
                document.isUseLastUpdatedDateInQuery(),
                document.getLastUpdatedDate()
        );
    }

    private ProcessorStatusDocument toDocument(ProcessorStatus domain) { // Updated return type and method name
        return new ProcessorStatusDocument(
                domain.getProcessorType(),
                domain.getPage(),
                domain.getPageSize(),
                domain.getCount(),
                domain.isUseLastUpdatedDateInQuery(),
                domain.getLastUpdatedDate()
        );
    }
}
