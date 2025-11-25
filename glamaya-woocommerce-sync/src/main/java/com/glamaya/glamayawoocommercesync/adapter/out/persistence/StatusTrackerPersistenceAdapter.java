package com.glamaya.glamayawoocommercesync.adapter.out.persistence;

import com.glamaya.glamayawoocommercesync.adapter.out.persistence.entity.ProcessorStatusDocument;
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatus;
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import com.glamaya.glamayawoocommercesync.port.out.StatusTrackerStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * An outbound adapter that implements the {@link StatusTrackerStore} port.
 * This adapter is responsible for translating domain {@link ProcessorStatus} objects
 * into persistence-specific {@link ProcessorStatusDocument} entities and interacting
 * with the {@link ProcessorStatusDocumentRepository} to store and retrieve them.
 */
@Component
@RequiredArgsConstructor
public class StatusTrackerPersistenceAdapter implements StatusTrackerStore {

    private final ProcessorStatusDocumentRepository repository;

    /**
     * Retrieves an existing {@link ProcessorStatus} or creates a new one if it doesn't exist.
     *
     * @param type           The {@link ProcessorType} to retrieve/create the status for.
     * @param resetOnStartup Whether to reset the tracker's state if it's the first time it's being accessed on startup.
     * @param pageSize       The configured page size for the processor.
     * @return A {@link Mono} emitting the {@link ProcessorStatus}.
     */
    @Override
    public Mono<ProcessorStatus> getOrCreate(ProcessorType type, boolean resetOnStartup, long pageSize) {
        if (resetOnStartup) {
            return createNewTracker(type, pageSize).map(this::toDomain);
        }
        return repository.findById(type)
                .switchIfEmpty(createNewTracker(type, pageSize))
                .flatMap(existing -> existing.getPageSize() != pageSize ? createNewTracker(type, pageSize) : Mono.just(existing))
                .map(this::toDomain);
    }

    /**
     * Saves the current {@link ProcessorStatus} to the persistence layer.
     *
     * @param tracker The {@link ProcessorStatus} to save.
     * @return A {@link Mono} emitting the saved {@link ProcessorStatus}.
     */
    @Override
    public Mono<ProcessorStatus> save(ProcessorStatus tracker) {
        return repository.save(toDocument(tracker)).map(this::toDomain);
    }

    /**
     * Creates and saves a new {@link ProcessorStatusDocument} with initial values.
     *
     * @param type     The {@link ProcessorType} for the new tracker.
     * @param pageSize The page size for the new tracker.
     * @return A {@link Mono} emitting the newly created {@link ProcessorStatusDocument}.
     */
    private Mono<ProcessorStatusDocument> createNewTracker(ProcessorType type, long pageSize) {
        return repository.save(ProcessorStatusDocument.builder()
                .processorType(type)
                .page(1)
                .pageSize(pageSize)
                .count(0)
                .useLastUpdatedDateInQuery(false)
                .build());
    }

    /**
     * Converts a persistence-specific {@link ProcessorStatusDocument} to a domain {@link ProcessorStatus} object.
     *
     * @param document The {@link ProcessorStatusDocument} to convert.
     * @return The corresponding domain {@link ProcessorStatus} object.
     */
    private ProcessorStatus toDomain(ProcessorStatusDocument document) {
        return new ProcessorStatus(
                document.getProcessorType(),
                document.getPage(),
                document.getPageSize(),
                document.getCount(),
                document.isUseLastUpdatedDateInQuery(),
                document.getLastUpdatedDate()
        );
    }

    /**
     * Converts a domain {@link ProcessorStatus} object to a persistence-specific {@link ProcessorStatusDocument} entity.
     *
     * @param domain The domain {@link ProcessorStatus} to convert.
     * @return The corresponding persistence {@link ProcessorStatusDocument} entity.
     */
    private ProcessorStatusDocument toDocument(ProcessorStatus domain) {
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
