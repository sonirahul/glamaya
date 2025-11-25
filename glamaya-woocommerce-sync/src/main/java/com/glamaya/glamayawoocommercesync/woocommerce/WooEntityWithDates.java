package com.glamaya.glamayawoocommercesync.woocommerce;

/**
 * Marker interface for WooCommerce entities that expose date fields relevant for incremental synchronization.
 * Entities implementing this interface are expected to provide methods to access their
 * GMT modified date, local modified date, and created date.
 */
public interface WooEntityWithDates {
    /**
     * Retrieves the GMT modified date of the entity.
     *
     * @return The GMT modified date as a String.
     */
    String getDateModifiedGmt();

    /**
     * Retrieves the local modified date of the entity.
     *
     * @return The local modified date as a String.
     */
    String getDateModified();

    /**
     * Retrieves the created date of the entity.
     *
     * @return The created date as a String.
     */
    String getDateCreated();
}
