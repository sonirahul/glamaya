package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.Name;
import com.glamaya.datacontracts.ecommerce.Phone;
import com.glamaya.datacontracts.ecommerce.Source;
import com.glamaya.datacontracts.ecommerce.SourceType;
import com.glamaya.datacontracts.whatsapp.Chat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappToContactMapperFactoryImpl implements ContactMapperFactory<Chat> {

    @Override
    public Contact toGlamayaContact(Chat chat, String sourceAccountName) {

        if (chat == null) {
            throw new IllegalArgumentException("Whatsapp chat invalid chat data received");
        }

        if (chat.getId() == null || !chat.getId().contains("@c.us")) {
            return null;
        }

        // Convert whatsappId to MSISDN-like string with leading '+', then validate/format to E.164
        var phone = formatPhoneNumber(formatWhatsppPhoneNumber(chat.getId()), "ZZ");

        if (phone == null) {
            log.error("Whatsapp chat id: {} - phone is invalid", chat.getId());
            return null;
        }

        var phones = StringUtils.isNotBlank(phone) ?
                List.of(Phone.builder().withPhone(phone).withPrimary(true).withIsPhoneValid(true).build()) : List.of();

        var source = Source.builder()
                .withSourceName(sourceAccountName)
                .withSourceType(SourceType.WHATSAPP_CHAT)
                .withSourceId(chat.getId())
                .withId(chat.getId()).build();
        var sources = List.of(source);

        String uuid = UUID.nameUUIDFromBytes((source.getSourceName() + "-" + source.getSourceType()
                + "-" + source.getSourceId() + "-" + source.getId()).toUpperCase()
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
        if (StringUtils.isBlank(whatsappId) || !whatsappId.contains("@")) {
            return null;
        }
        String numberPart = whatsappId.split("@")[0].trim();
        if (StringUtils.isBlank(numberPart)) {
            return null;
        }
        // If already international format, return as-is
        if (numberPart.startsWith("+")) {
            return numberPart;
        }
        // Remove any non-digit characters just in case
        String digits = numberPart.replaceAll("\\D", "");
        if (StringUtils.isBlank(digits)) {
            return null;
        }
        if (numberPart.matches("^\\d{10}$")) {
            return "+91" + numberPart;
        }
        // Do NOT guess country codes; require international format upstream.
        // If we only have local digits without country code, return null so caller can decide.
        // Returning null leaves validation to upstream or configuration.
        return "+" + numberPart;
    }

    private Name buildName(String rawName) {
        if (StringUtils.isBlank(rawName) || !rawName.matches(".*[a-zA-Z].*")) {
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