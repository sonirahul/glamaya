package com.glamaya.glamayawoocommercesync.port.out;

import reactor.core.publisher.Flux;
import java.util.Map;

public interface WooCommerceApiClientPort {
    Flux<Object> fetch(String url, Map<String, String> queryParams, String oauthHeader);
}
