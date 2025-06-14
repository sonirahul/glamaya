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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

@Service
@RequiredArgsConstructor
public class WooUserToContactMapperFactoryImpl implements ContactMapperFactory<User> {

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

        var email = user.getBilling().getEmail();
        var phone = user.getBilling().getPhone();

        var emails = StringUtils.hasText(email) ?
                List.of(Email.builder().withEmail(email).withPrimary(true)
                        .withIsEmailValid(user.getBilling().getIsEmailValid()).build()) : List.of();
        var phones = StringUtils.hasText(phone) ?
                List.of(Phone.builder().withPhone(phone).withPrimary(true)
                        .withIsPhoneValid(user.getBilling().getIsPhoneValid()).build()) : List.of();

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
                .withMergedIds(List.of(uuid))
                .withId(uuid)
                .withName(buildName(user))
                // GMT date is used to ensure consistency with other date fields
                .withCreatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(user.getDateCreatedGmt()))
                .withUpdatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(user.getDateModifiedGmt()))
                .build();
    }

    private Address buildAddress(com.glamaya.datacontracts.woocommerce.Address address, AddressType type) {
        if (address == null) {
            return null;
        }

        return Address.builder()
                .withType(type)
                .withAddressLine1(address.getAddress1())
                .withAddressLine2(address.getAddress2())
                .withCity(address.getCity())
                .withState(address.getState())
                .withCountry(address.getCountry())
                .withZipCode(address.getPostcode())
                .withFirstName(address.getFirstName())
                .withLastName(address.getLastName())
                .withPhone(address.getPhone())
                .withIsPhoneValid(address.getIsPhoneValid())
                .withEmail(address.getEmail())
                .withIsEmailValid(address.getIsEmailValid())
                .build();
    }

    private Name buildName(User user) {
        return Name.builder()
                .withFirst(user.getFirstName())
                .withLast(user.getLastName())
                .withFull(user.getFirstName() + " " + user.getLastName())
                .build();
    }
}