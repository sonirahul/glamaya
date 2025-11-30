package com.glamaya.sync.runner.adapter.store;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProcessorStatusMapper {

    ProcessorStatus toDomain(ProcessorStatusDocument document);

    ProcessorStatusDocument toDocument(ProcessorStatus domain);
}
