package com.glamaya.sync.platform.whatsapp.adapter.util;

import java.time.Instant;
import java.util.function.LongFunction;

/**
 * Common date parsing utilities for WhatsApp payloads.
 */
public final class WhatsappDateParsingUtils {
    private WhatsappDateParsingUtils() {}

    /**
     * Converts a UNIX timestamp (seconds since epoch) to an Instant.
     */
    public static final LongFunction<Instant> PARSE_UNIX_TIMESTAMP_TO_INSTANT = Instant::ofEpochSecond;
}
