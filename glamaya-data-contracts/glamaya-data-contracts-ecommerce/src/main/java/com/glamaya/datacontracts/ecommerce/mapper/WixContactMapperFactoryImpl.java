package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.Address;
import com.glamaya.datacontracts.ecommerce.AddressType;
import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.Email;
import com.glamaya.datacontracts.ecommerce.Name;
import com.glamaya.datacontracts.ecommerce.Phone;
import com.glamaya.datacontracts.ecommerce.PurchaseItem;
import com.glamaya.datacontracts.ecommerce.Source;
import com.glamaya.datacontracts.ecommerce.SourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WixContactMapperFactoryImpl implements ContactMapperFactory<com.glamaya.datacontracts.wix.Contact> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'");

    @Override
    public Contact toGlamayaContact(com.glamaya.datacontracts.wix.Contact user, String sourceAccountName) {

        if (user == null || user.getPrimaryInfo() == null) {
            throw new IllegalArgumentException("Wix contact invalid user data received");
        }

        var name = buildName(user.getInfo().getName());
        var addresses = buildAddresses(user, name);

        var email = formatEmail(user.getPrimaryInfo().getEmail());
        var phone = formatPhoneNumber(user.getPrimaryInfo().getPhone(), findCountryCode(addresses));

        if (email == null && phone == null) {
            throw new IllegalArgumentException(String.format("Wix contact id: %s - email and phone are null", user.getId()));
        }

        var emails = StringUtils.hasText(email) ?
                List.of(Email.builder().withEmail(email).withPrimary(true).build()) : List.of();
        var phones = StringUtils.hasText(phone) ?
                List.of(Phone.builder().withPhone(phone).withPrimary(true).build()) : List.of();

        addresses.forEach(address -> {
            address.setPhone(phone);
            address.setEmail(email);
        });

        var extendedFields = buildExtendedFields(user);

        var sources = List.of(Source.builder()
                .withSourceName(sourceAccountName)
                .withSourceType(SourceType.WIX)
                .withSourceId(user.getId())
                .withId(user.getId())
                .build());

        return Contact.builder()
                .withEmails(emails)
                .withPhones(phones)
                .withAddresses(addresses)
                .withSources(sources)
                .withMergedIds(List.of(user.getId()))
                .withId(user.getId())
                .withName(name)
                .withCreatedDate(user.getCreatedDate())
                .withUpdatedDate(user.getUpdatedDate())
                .withPurchaseHistory(List.of(extendedFields))
                .build();
    }

    private PurchaseItem buildExtendedFields(com.glamaya.datacontracts.wix.Contact user) {
        var wixExtendedFields = user.getInfo().getExtendedFields().getItems();

        return PurchaseItem.builder()
                .withNumOfPurchases(wixExtendedFields.getEcomNumOfPurchases())
                .withLastPurchaseDate(wixExtendedFields.getEcomLastPurchaseDate())
                .withTotalSpentAmount(wixExtendedFields.getEcomTotalSpentAmount())
                .withTotalSpentCurrency(wixExtendedFields.getEcomTotalSpentCurrency())
                .build();
    }

    private List<Address> buildAddresses(com.glamaya.datacontracts.wix.Contact user, Name name) {
        if (Objects.isNull(user.getInfo().getAddresses()) || CollectionUtils.isEmpty(user.getInfo().getAddresses().getItems())) {
            return List.of();
        }

        return user.getInfo().getAddresses().getItems().stream()
                .map(addressItem -> {
                    AddressType addressType = addressItem.getTag().equalsIgnoreCase(AddressType.SHIPPING.toString())
                            ? AddressType.SHIPPING : AddressType.BILLING;
                    return buildAddress(addressItem.getAddress(), addressType, name);
                })
                .toList();
    }

    private Address buildAddress(com.glamaya.datacontracts.wix.Address address, AddressType type, Name name) {

        if (address == null) {
            return null;
        }

        String state = address.getSubdivisions().isEmpty() ? null : address.getSubdivisions().getFirst().getName();

        return Address.builder()
                .withType(type)
                .withAddressLine1(address.getAddressLine())
                .withCity(toUpperCamelCase(address.getCity()))
                .withCountry(address.getCountry())
                .withFirstName(Objects.nonNull(name) ? name.getFirst() : null)
                .withLastName(Objects.nonNull(name) ? name.getLast() : null)
                .withZipCode(address.getPostalCode())
                .withState(toUpperCamelCase(state))
                .build();
    }

    private Name buildName(com.glamaya.datacontracts.wix.Name name) {
        if (name == null) {
            return null;
        }
        return Name.builder()
                .withFirst(toUpperCamelCase(name.getFirst()))
                .withLast(toUpperCamelCase(name.getLast()))
                .withFull(toUpperCamelCase(name.getFirst() + " " + name.getLast()))
                .build();
    }
}
