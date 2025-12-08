package com.glamaya.sync.platform.whatsapp.adapter.client;

import com.glamaya.sync.core.domain.model.ProcessorStatus;
import com.glamaya.sync.platform.whatsapp.common.LoggerConstants;
import com.glamaya.sync.platform.whatsapp.adapter.client.descriptor.WhatsappEntityDescriptor;
import com.glamaya.sync.platform.whatsapp.config.APIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * A generic service for interacting with the WAHA WhatsApp API in a reactive way.
 * It uses a descriptor pattern to fetch different types of entities.
 * Authentication is provided via X-Api-Key header (no OAuth1).
 *
 * @param <E> The type of the entity to fetch.
 */
@Slf4j
@Service
public class WhatsappApiService<E> {

    private final WebClient webClient;
    private final String apiKey;

    /**
     * Constructs the WhatsappApiService with a WebClient and API key.
     *
     * @param webClient The WebClient instance for HTTP requests.
     * @param apiKey    The API key for authentication.
     */
    public WhatsappApiService(
            @Qualifier("whatsappWebClient") WebClient webClient,
            @Value("${glamaya.sync.whatsapp.api.apiKey}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    /**
     * Fetches a single page of entities from the WAHA WhatsApp API.
     *
     * @param descriptor The descriptor defining the entity-specific details.
     * @param queryParams Query parameters for the request.
     * @param status     The current processor status, containing the page number.
     * @param config     The API configuration, containing the URL and page size.
     * @return A Flux emitting the entities found on the specified page.
     */
    public Flux<E> fetchPage(WhatsappEntityDescriptor<E> descriptor, Map<String, String> queryParams, ProcessorStatus status, APIConfig config) {
        String relativeUrl = config.getQueryUrl();

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(relativeUrl);
                    if (queryParams != null) {
                        queryParams.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.build();
                })
                .header("X-Api-Key", apiKey)
                .header(HttpHeaders.ACCEPT, "*/*")
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("<empty body>")
                                .flatMap(body -> {
                                    String procName = descriptor != null ? descriptor.getClass().getSimpleName() : "WhatsappApiService";
                                    log.error(LoggerConstants.WC_API_ERROR, procName, resp.statusCode(), body);
                                    return Mono.error(new RuntimeException("Remote API Error: " + resp.statusCode() + " - " + body));
                                }))
                .bodyToMono(descriptor.getListTypeReference())
                .flatMapMany(list -> list == null ? Flux.empty() : Flux.fromIterable(list))
                .onErrorResume(DecodingException.class, e -> {
                    String procName = descriptor.getClass().getSimpleName();
                    log.error(LoggerConstants.WC_API_JSON_ERROR, procName, status.getNextPage(), e.getMessage());
                    return Flux.empty();
                });
    }
}
