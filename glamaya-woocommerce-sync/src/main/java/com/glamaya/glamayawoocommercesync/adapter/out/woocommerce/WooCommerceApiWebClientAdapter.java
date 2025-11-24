package com.glamaya.glamayawoocommercesync.adapter.out.woocommerce;

import com.glamaya.glamayawoocommercesync.port.out.WooCommerceApiClientPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class WooCommerceApiWebClientAdapter implements WooCommerceApiClientPort {

    private final WebClient webClient;

    public WooCommerceApiWebClientAdapter(@Qualifier("woocommerceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Flux<Object> fetch(String url, Map<String, String> queryParams, String oauthHeader) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(url);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, oauthHeader == null ? "" : oauthHeader)
                .retrieve()
                .onStatus(status -> status.is5xxServerError() || status.value() == 429,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("<empty>")
                                .flatMap(body -> Mono.error(new RuntimeException("Remote HTTP " + resp.statusCode().value() + ": " + body))))
                .bodyToFlux(Object.class);
    }
}
