package com.glamaya.glamayawoocommercesync.port.out;

import java.util.Map;

public interface OAuthSignerPort {
    String generateOAuth1Header(String url, Map<String, String> queryParams);
}
