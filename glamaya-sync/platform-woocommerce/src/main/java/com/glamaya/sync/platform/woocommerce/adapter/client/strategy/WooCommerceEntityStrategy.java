package com.glamaya.sync.platform.woocommerce.adapter.client.strategy;

import org.springframework.core.ParameterizedTypeReference;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * A strategy interface that defines the entity-specific details required by the generic
 * WooCommerceApiService to fetch data.
 *
 * @param <E> The type of the entity (e.g., Order, Product).
 */
public interface WooCommerceEntityStrategy<E> {

    /**
     * Provides the ParameterizedTypeReference needed by WebClient to deserialize a list of entities.
     *
     * @return The type reference for a list of the entity type.
     */
    ParameterizedTypeReference<List<E>> getListTypeReference();

    /**
     * Provides a function that can extract the "last modified" date (as an Instant)
     * from an entity object.
     *
     * @return A function to extract the last modified date.
     */
    Function<E, Instant> getLastModifiedExtractor();
}
