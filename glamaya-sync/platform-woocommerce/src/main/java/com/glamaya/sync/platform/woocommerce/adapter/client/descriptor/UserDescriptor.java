package com.glamaya.sync.platform.woocommerce.adapter.client.descriptor;

import com.glamaya.datacontracts.woocommerce.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static com.glamaya.sync.platform.woocommerce.adapter.util.WooDateParsingUtils.PARSE_ISO_LOCAL_DATE_TIME_TO_INSTANT;

/**
 * The concrete descriptor for fetching WooCommerce User entities.
 */
@Component
public class UserDescriptor implements WooCommerceEntityDescriptor<User> {

    private static final ParameterizedTypeReference<List<User>> USER_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    @Override
    public ParameterizedTypeReference<List<User>> getListTypeReference() {
        return USER_LIST_TYPE;
    }

    @Override
    public Function<User, Instant> getLastModifiedExtractor() {
        return user -> PARSE_ISO_LOCAL_DATE_TIME_TO_INSTANT.apply(user.getDateModifiedGmt());
    }
}
