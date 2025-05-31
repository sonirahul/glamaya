package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.Name;
import com.glamaya.datacontracts.ecommerce.Phone;
import com.glamaya.datacontracts.ecommerce.Source;
import com.glamaya.datacontracts.ecommerce.SourceType;
import com.glamaya.datacontracts.whatsapp.Chat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WhatsappToContactMapperFactoryImpl implements ContactMapperFactory<Chat> {

    @Override
    public Contact toGlamayaContact(Chat chat, String sourceAccountName) {

        if (chat == null) {
            throw new IllegalArgumentException("Whatsapp chat invalid chat data received");
        }

        var phone = formatPhoneNumber(formatWhatsppPhoneNumber(chat.getId()), "ZZ");

        if (phone == null) {
            throw new IllegalArgumentException(String.format("Whatsapp chat id: %s - phone is null", chat.getId()));
        }

        var phones = StringUtils.hasText(phone) ?
                List.of(Phone.builder().withPhone(phone).withPrimary(true).build()) : List.of();

        var source = Source.builder()
                .withSourceName(sourceAccountName)
                .withSourceType(SourceType.WHATSAPP_CHAT)
                .withSourceId(chat.getId())
                .withUserId(chat.getId()).build();
        var sources = List.of(source);

        String uuid = UUID.nameUUIDFromBytes((source.getSourceName() + "-" + source.getSourceType()
                + "-" + source.getSourceId() + "-" + source.getUserId()).toUpperCase()
                .getBytes(StandardCharsets.UTF_8)).toString();

        return Contact.builder()
                .withPhones(phones)
                .withSources(sources)
                .withMergedIds(List.of(uuid))
                .withId(uuid)
                .withName(buildName(chat.getName()))
                // GMT date is used to ensure consistency with other date fields
                .withUpdatedDate(chat.getConversationDateTime())
                .build();
    }

    public String formatWhatsppPhoneNumber(String whatsappId) {
        if (whatsappId == null || !whatsappId.contains("@")) {
            return null;
        }
        return "+" + whatsappId.split("@")[0];
    }

    private Name buildName(String rawName) {
        if (!StringUtils.hasText(rawName) || !rawName.matches(".*[a-zA-Z].*")) {
            return null;
        }
        String[] parts = rawName.trim().split("\\s+");
        if (parts.length == 1) {
            return Name.builder()
                    .withFirst(parts[0])
                    .withFull(parts[0])
                    .build();
        } else {
            String first = parts[0];
            String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            return Name.builder()
                    .withFirst(first)
                    .withLast(last)
                    .withFull(rawName.trim())
                    .build();
        }
    }
}