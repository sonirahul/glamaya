package com.glamaya.sync.platform.woocommerce.adapter.client;

import com.glamaya.sync.platform.woocommerce.adapter.client.descriptor.WooCommerceEntityDescriptor;
import com.glamaya.sync.platform.woocommerce.config.APIConfig;
import com.glamaya.sync.platform.woocommerce.port.out.OAuthSignerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * A generic service for interacting with the WooCommerce API in a reactive way.
 * It uses a descriptor pattern to fetch different types of entities (Orders, Products, etc.).
 *
 * @param <E> The type of the entity to fetch.
 */
@Slf4j
@Service
public class WooCommerceApiService<E> {

    private final WebClient webClient;
    private final OAuthSignerPort oAuthSigner;

    public WooCommerceApiService(
            WebClient webClient,
            OAuthSignerPort oAuthSigner) {
        this.webClient = webClient;
        this.oAuthSigner = oAuthSigner;
    }

    /**
     * Fetches a single page of entities from the WooCommerce API.
     *
     * @param descriptor The descriptor defining the entity-specific details.
     * @param status     The current processor status, containing the page number.
     * @param config     The API configuration, containing the URL and page size.
     * @return A Flux emitting the entities found on the specified page.
     */
    public Flux<E> fetchPage(WooCommerceEntityDescriptor<E> descriptor, Map<String, String> queryParams, com.glamaya.sync.core.domain.model.ProcessorStatus status, APIConfig config) {
        String relativeUrl = config.getQueryUrl();
        String oauthHeader = oAuthSigner.generateOAuth1Header(relativeUrl, queryParams);

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(relativeUrl);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, oauthHeader)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("<empty body>")
                                .flatMap(body -> {
                                    log.error("WooCommerce API Error: {} - {}", resp.statusCode(), body);
                                    return Mono.error(new RuntimeException("Remote API Error: " + resp.statusCode() + " - " + body));
                                }))
                .bodyToMono(descriptor.getListTypeReference())
                .flatMapMany(list -> list == null ? Flux.empty() : Flux.fromIterable(list))
                .onErrorResume(DecodingException.class, e -> {
                    log.error("WebClient-level JSON decoding error. Returning empty Flux for page {}. Error: {}",
                            status.getNextPage(), e.getMessage());
                    return Flux.empty();
                });
    }
}
