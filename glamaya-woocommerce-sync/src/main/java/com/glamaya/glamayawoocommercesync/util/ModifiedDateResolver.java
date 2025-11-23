package com.glamaya.glamayawoocommercesync.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Resolves the most appropriate modified date from available WooCommerce date fields.
 * Priority: modifiedGmt -> modified -> created -> now.
 */
public final class ModifiedDateResolver {
    private ModifiedDateResolver() {
    }

    public static String resolve(String modifiedGmt, String modified, String created) {
        if (nonEmpty(modifiedGmt)) return modifiedGmt;
        if (nonEmpty(modified)) return modified;
        if (nonEmpty(created)) return created;
        return Instant.now().toString();
    }

    public static Instant toInstantSafe(String iso) {
        try {
            if (iso == null || iso.isBlank()) return Instant.now();
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            return Instant.now();
        }
    }

    private static boolean nonEmpty(String s) {
        return s != null && !s.isBlank();
    }
}
