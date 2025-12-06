package com.glamaya.sync.platform.woocommerce.adapter.util;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

/**
 * Common date parsing utilities for WooCommerce payloads.
 */
public final class WooDateParsingUtils {
    private WooDateParsingUtils() {}

    public static final Function<String, Instant> PARSE_ISO_LOCAL_DATE_TIME_TO_INSTANT = date ->
            Optional.ofNullable(date).filter(StringUtils::hasText)
                    .map(d -> LocalDateTime.parse(d, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC))
                    .orElse(null);
}
