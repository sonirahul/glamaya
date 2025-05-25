package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.Address;
import com.glamaya.datacontracts.ecommerce.AddressType;
import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.Email;
import com.glamaya.datacontracts.ecommerce.Name;
import com.glamaya.datacontracts.ecommerce.Phone;
import com.glamaya.datacontracts.ecommerce.Source;
import com.glamaya.datacontracts.ecommerce.SourceType;
import com.glamaya.datacontracts.woocommerce.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WooUserToContactMapperFactoryImpl implements ContactMapperFactory<User> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public Contact toGlamayaContact(User user, String sourceAccountName) {

        if (user == null) {
            throw new IllegalArgumentException("Woocommerce user invalid user data received");
        }

        var addresses =Map.of(
                        AddressType.BILLING, user.getBilling(),
                        AddressType.SHIPPING, user.getShipping()
                )
                .entrySet().stream()
                .map(entry -> buildAddress(entry.getValue(), entry.getKey()))
                .filter(Objects::nonNull)
                .toList();

        var email = formatEmail(user.getEmail());
        var phone = formatPhoneNumber(Objects.nonNull(user.getBilling()) ?
                user.getBilling().getPhone() : null, findCountryCode(addresses));

        if (email == null && phone == null) {
            throw new IllegalArgumentException(String.format("Woocommerce user id: %d - email and phone are null", user.getId()));
        }

        var emails = StringUtils.hasText(email) ?
                List.of(Email.builder().withEmail(email).withPrimary(true).build()) : List.of();
        var phones = StringUtils.hasText(phone) ?
                List.of(Phone.builder().withPhone(phone).withPrimary(true).build()) : List.of();

        addresses.forEach(address -> {
            address.setPhone(phone);
            address.setEmail(email);
        });

        var source = Source.builder()
                .withSourceName(sourceAccountName)
                .withSourceType(SourceType.WOOCOMMERCE_USER)
                .withSourceId(user.getId())
                .withUserId(user.getId()).build();
        var sources = List.of(source);

        String uuid = UUID.nameUUIDFromBytes((source.getSourceName() + "-" + source.getSourceType()
                + "-" + source.getSourceId() + "-" + source.getUserId()).toUpperCase()
                .getBytes(StandardCharsets.UTF_8)).toString();

        return Contact.builder()
                .withEmails(emails)
                .withPhones(phones)
                .withAddresses(addresses)
                .withSources(sources)
                .withId(uuid)
                .withName(buildName(user))
                .withCreatedDate(StringUtils.hasText(user.getDateCreated()) ? LocalDateTime.parse(user.getDateCreated(), FORMATTER) : null)
                .withUpdatedDate(StringUtils.hasText(user.getDateModified()) ? LocalDateTime.parse(user.getDateModified(), FORMATTER) : null)
                .build();
    }

    private boolean isAddressEmpty(com.glamaya.datacontracts.woocommerce.Address address) {
        return address == null ||
                (address.getAddress1() == null &&
                        address.getAddress2() == null &&
                        address.getCity() == null &&
                        address.getCountry() == null &&
                        address.getFirstName() == null &&
                        address.getLastName() == null &&
                        address.getPhone() == null &&
                        address.getPostcode() == null &&
                        address.getState() == null);
    }

    private Address buildAddress(com.glamaya.datacontracts.woocommerce.Address address, AddressType type) {
        if (isAddressEmpty(address)) {
            return null;
        }

        return Address.builder()
                .withType(type)
                .withAddressLine1(address.getAddress1())
                .withAddressLine2(address.getAddress2())
                .withCity(toUpperCamelCase(address.getCity()))
                .withCountry(address.getCountry())
                .withFirstName(address.getFirstName())
                .withLastName(address.getLastName())
                .withZipCode(address.getPostcode())
                .withState(toUpperCamelCase(address.getState()))
                .build();
    }

    private Name buildName(User user) {
        return Name.builder()
                .withFirst(toUpperCamelCase(user.getFirstName()))
                .withLast(toUpperCamelCase(user.getLastName()))
                .withFull(toUpperCamelCase(user.getFirstName() + " " + user.getLastName()))
                .build();
    }
}