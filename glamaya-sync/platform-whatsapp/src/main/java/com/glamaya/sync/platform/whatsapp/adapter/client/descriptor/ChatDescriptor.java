package com.glamaya.sync.platform.whatsapp.adapter.client.descriptor;

import com.glamaya.datacontracts.whatsapp.Chat;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static com.glamaya.sync.platform.whatsapp.adapter.util.WhatsappDateParsingUtils.PARSE_UNIX_TIMESTAMP_TO_INSTANT;

/**
 * Descriptor for fetching WhatsApp User entities.
 * <p>
 * Provides type information and last-modified extraction logic for WhatsApp Chat entities.
 */
@Component
public class ChatDescriptor implements WhatsappEntityDescriptor<Chat> {

    /**
     * Type reference for a list of WhatsApp Chat entities.
     */
    private static final ParameterizedTypeReference<List<Chat>> USER_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    /**
     * Returns the type reference for a list of WhatsApp Chat entities.
     *
     * @return ParameterizedTypeReference for List<Chat>
     */
    @Override
    public ParameterizedTypeReference<List<Chat>> getListTypeReference() {
        return USER_LIST_TYPE;
    }

    /**
     * Extracts the last modified timestamp from a WhatsApp Chat entity.
     *
     * @return Function to extract Instant from Chat's conversationTimestamp
     */
    @Override
    public Function<Chat, Instant> getLastModifiedExtractor() {
        return chat -> PARSE_UNIX_TIMESTAMP_TO_INSTANT.apply(chat.getConversationTimestamp());
    }
}
