package com.glamaya.sync.platform.whatsapp.adapter.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.whatsapp.Chat;
import com.glamaya.datacontracts.whatsapp.ChatSearchRequest;
import com.glamaya.datacontracts.whatsapp.SortBy;
import com.glamaya.datacontracts.whatsapp.SortOrder;
import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.core.domain.model.SyncContext;
import com.glamaya.sync.core.domain.port.out.DataProvider;
import com.glamaya.sync.platform.whatsapp.adapter.client.descriptor.UserDescriptor;
import com.glamaya.sync.platform.whatsapp.adapter.util.WhatsappPagination;
import com.glamaya.sync.platform.whatsapp.config.APIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Implementation of DataProvider for fetching WhatsApp Users.
 * This class orchestrates fetching a page and updating the status based on the result.
 */
@Slf4j
@Component
public class WhatsappUserDataProvider implements DataProvider<Chat> {

    private final WhatsappApiService<Chat> apiService;
    private final UserDescriptor userDescriptor;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the WhatsappUserDataProvider with required dependencies.
     *
     * @param apiService     The API service for WhatsApp data fetching.
     * @param userDescriptor The descriptor for WhatsApp user entities.
     * @param objectMapper   The object mapper for query param conversion.
     */
    public WhatsappUserDataProvider(WhatsappApiService<Chat> apiService, UserDescriptor userDescriptor, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.userDescriptor = userDescriptor;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches WhatsApp Chat data for the given sync context.
     *
     * @param context The sync context containing configuration and status.
     * @return A Flux of Chat entities for the requested page.
     */
    @Override
    public Flux<Chat> fetchData(SyncContext<?> context) {
        var config = (APIConfig) context.configuration().get();
        var status = context.status();
        var queryParams = buildQueryParams(status, config);

        return apiService.fetchPage(userDescriptor, queryParams, status, config)
                .collectList()
                .doOnNext(pageItems -> WhatsappPagination.updateStatusAfterPage(status, pageItems, config,
                        userDescriptor.getLastModifiedExtractor()))
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Builds query parameters for WhatsApp Chat search requests based on status and config.
     *
     * @param statusTracker The processor status tracker.
     * @param config        The API configuration.
     * @return Map of query parameters for the API request.
     */
    private Map<String, String> buildQueryParams(ProcessorStatus statusTracker, APIConfig config) {

        var builder = ChatSearchRequest.builder()
                .withLimit(Long.valueOf(config.getPageSize()))
                .withOffset((long) (statusTracker.getNextPage() - 1) * config.getPageSize())
                .withSortOrder(SortOrder.asc)
                .withSortBy(SortBy.conversationTimestamp);

        return objectMapper.convertValue(builder.build(), new TypeReference<>() {
        });
    }
}
