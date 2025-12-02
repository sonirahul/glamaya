package com.glamaya.sync.platform.woocommerce.adapter.client.descriptor;

import com.glamaya.datacontracts.woocommerce.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The concrete descriptor for fetching WooCommerce User entities.
 */
@Component
public class UserDescriptor implements WooCommerceEntityDescriptor<User> {

    private static final ParameterizedTypeReference<List<User>> USER_LIST_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final Function<String, Instant> STRING_DATE_TO_INSTANT = date ->
            Optional.ofNullable(date).filter(StringUtils::hasText)
                    .map(d -> LocalDateTime.parse(d, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC))
                    .orElse(null);

    @Override
    public ParameterizedTypeReference<List<User>> getListTypeReference() {
        return USER_LIST_TYPE;
    }

    @Override
    public Function<User, Instant> getLastModifiedExtractor() {
        return user -> STRING_DATE_TO_INSTANT.apply(user.getDateModifiedGmt());
    }
}
