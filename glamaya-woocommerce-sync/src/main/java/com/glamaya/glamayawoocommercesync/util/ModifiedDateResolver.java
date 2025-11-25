package com.glamaya.glamayawoocommercesync.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Utility class for resolving and parsing date strings, primarily for WooCommerce entities.
 * It provides methods to determine the most relevant "modified" date from a set of available
 * date fields and to safely convert ISO date strings to {@link Instant} objects.
 */
public final class ModifiedDateResolver {
    // Private constructor to prevent instantiation of utility class
    private ModifiedDateResolver() {
    }

    /**
     * Resolves the most appropriate modified date string from a set of WooCommerce date fields.
     * The priority order is: {@code modifiedGmt} -> {@code modified} -> {@code created}.
     * If none of these are present, the current {@link Instant} is returned as a string.
     *
     * @param modifiedGmt The GMT modified date string.
     * @param modified    The local modified date string.
     * @param created     The created date string.
     * @return The resolved date string, or {@link Instant#now()} as a string if no valid date is found.
     */
    public static String resolve(String modifiedGmt, String modified, String created) {
        if (nonEmpty(modifiedGmt)) return modifiedGmt;
        if (nonEmpty(modified)) return modified;
        if (nonEmpty(created)) return created;
        return Instant.now().toString();
    }

    /**
     * Safely converts an ISO 8601 date string to an {@link Instant}.
     * If the input string is null, blank, or cannot be parsed, {@link Instant#now()} is returned.
     *
     * @param iso The ISO 8601 date string to parse.
     * @return An {@link Instant} representing the parsed date, or {@link Instant#now()} on error or invalid input.
     */
    public static Instant toInstantSafe(String iso) {
        try {
            if (iso == null || iso.isBlank()) return Instant.now();
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            // Log the parsing error if necessary, but return Instant.now() to avoid crashing
            return Instant.now();
        }
    }

    /**
     * Checks if a given string is not null and not blank.
     *
     * @param s The string to check.
     * @return {@code true} if the string is not null and not blank, {@code false} otherwise.
     */
    private static boolean nonEmpty(String s) {
        return s != null && !s.isBlank();
    }
}
