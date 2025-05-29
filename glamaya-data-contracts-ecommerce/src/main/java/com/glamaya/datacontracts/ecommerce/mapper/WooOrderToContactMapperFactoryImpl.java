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
import com.glamaya.datacontracts.woocommerce.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

@Service
@RequiredArgsConstructor
public class WooOrderToContactMapperFactoryImpl implements ContactMapperFactory<Order> {

    @Override
    public Contact toGlamayaContact(Order order, String sourceAccountName) {

        if (order == null) {
            throw new IllegalArgumentException("Woocommerce order invalid order data received");
        }

        var addresses =Map.of(
                        AddressType.BILLING, order.getBilling(),
                        AddressType.SHIPPING, order.getShipping()
                )
                .entrySet().stream()
                .map(entry -> buildAddress(entry.getValue(), entry.getKey()))
                .filter(Objects::nonNull)
                .toList();

        var email = formatEmail(order.getBilling().getEmail());
        var phone = formatPhoneNumber(Objects.nonNull(order.getBilling()) ?
                order.getBilling().getPhone() : null, findCountryCode(addresses));

        if (email == null && phone == null) {
            throw new IllegalArgumentException(String.format("Woocommerce order id: %d - email and phone are null", order.getId()));
        }

        var emails = StringUtils.hasText(email) ?
                List.of(Email.builder().withEmail(email).withPrimary(true).build()) : List.of();
        var phones = StringUtils.hasText(phone) ?
                List.of(Phone.builder().withPhone(phone).withPrimary(true).build()) : List.of();

        addresses.forEach(address -> {
            address.setPhone(phone);
            address.setEmail(email);
        });

        var extendedFields = PurchaseItem.builder()
                .withNumOfPurchases(1L)
                // GMT date is used to ensure consistency with other date fields
                .withLastPurchaseDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(order.getDateCreatedGmt()))
                .withTotalSpentAmount(BigDecimal.valueOf(Double.parseDouble(order.getTotal())))
                .withTotalSpentCurrency(order.getCurrency())
                .build();

        var source = Source.builder()
                .withSourceName(sourceAccountName)
                .withSourceType(SourceType.WOOCOMMERCE_ORDER)
                .withSourceId(String.valueOf(order.getId()))
                .withUserId(order.getCustomerId()).build();
        var sources = List.of(source);

        String uuid = UUID.nameUUIDFromBytes((source.getSourceName() + "-" + source.getSourceType()
                + "-" + source.getSourceId() + "-" + source.getUserId()).toUpperCase()
                .getBytes(StandardCharsets.UTF_8)).toString();

        return Contact.builder()
                .withPhones(phones)
                .withEmails(emails)
                .withAddresses(addresses)
                .withSources(sources)
                .withMergedIds(List.of(uuid))
                .withId(uuid)
                .withName(buildName(order))
                // GMT date is used to ensure consistency with other date fields
                .withCreatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(order.getDateCreatedGmt()))
                .withUpdatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(order.getDateModifiedGmt()))
                .withPurchaseHistory(List.of(extendedFields))
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

    private Name buildName(Order order) {
        return Name.builder()
                .withFirst(toUpperCamelCase(order.getBilling().getFirstName()))
                .withLast(toUpperCamelCase(order.getBilling().getLastName()))
                .withFull(toUpperCamelCase(order.getBilling().getFirstName() + " " + order.getBilling().getLastName()))
                .build();
    }
}