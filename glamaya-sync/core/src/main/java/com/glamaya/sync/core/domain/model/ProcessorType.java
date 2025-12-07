package com.glamaya.sync.core.domain.model;

/**
 * An enum that uniquely identifies each distinct synchronization process.
 * This is used for status tracking, metrics, and logging.
 */
public enum ProcessorType {
    WOOCOMMERCE_ORDER,
    WOOCOMMERCE_ORDER_TO_CONTACT,
    WOOCOMMERCE_PRODUCT,
    WOOCOMMERCE_USER,

    WIX_ORDER,
    WIX_PRODUCT,
    WIX_CONTACT
}
