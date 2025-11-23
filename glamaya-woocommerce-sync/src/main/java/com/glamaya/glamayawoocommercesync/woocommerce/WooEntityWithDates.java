package com.glamaya.glamayawoocommercesync.woocommerce;

/**
 * Marker interface for Woo entities exposing date fields used for incremental sync.
 */
public interface WooEntityWithDates {
    String getDateModifiedGmt();

    String getDateModified();

    String getDateCreated();
}

