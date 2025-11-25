package com.glamaya.glamayawoocommercesync.config.webclient;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

/**
 * Custom {@link Qualifier} annotation to distinguish between different {@link org.springframework.web.reactive.function.client.WebClient}
 * beans that are specifically configured for sending webhook notifications.
 * This helps in injecting the correct {@code WebClient} instance where multiple are defined.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface WebhookTarget {
}
