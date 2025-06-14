package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.Address;
import com.glamaya.datacontracts.ecommerce.AddressType;
import com.glamaya.datacontracts.ecommerce.Contact;
import com.glamaya.datacontracts.ecommerce.Email;
import com.glamaya.datacontracts.ecommerce.Name;
import com.glamaya.datacontracts.ecommerce.OrderStatus;
import com.glamaya.datacontracts.ecommerce.PaymentStatus;
import com.glamaya.datacontracts.ecommerce.Phone;
import com.glamaya.datacontracts.ecommerce.PurchaseItem;
import com.glamaya.datacontracts.ecommerce.Source;
import com.glamaya.datacontracts.ecommerce.SourceType;
import com.glamaya.datacontracts.woocommerce.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class WooOrderToContactMapperFactoryImpl implements ContactMapperFactory<Order> {

    @Override
    public Contact toGlamayaContact(Order order, String sourceAccountName) {

        if (order == null) {
            throw new IllegalArgumentException("Woocommerce order invalid order data received");
        }

        var addresses = Map.of(
                        AddressType.BILLING, order.getBilling(),
                        AddressType.SHIPPING, order.getShipping()
                )
                .entrySet().stream()
                .map(entry -> buildAddress(entry.getValue(), entry.getKey()))
                .filter(Objects::nonNull)
                .toList();

        var email = order.getBilling().getEmail();
        var phone = order.getBilling().getPhone();

        var emails = StringUtils.hasText(email) ?
                List.of(Email.builder().withEmail(email).withPrimary(true)
                        .withIsEmailValid(order.getBilling().getIsEmailValid()).build()) : List.of();
        var phones = StringUtils.hasText(phone) ?
                List.of(Phone.builder().withPhone(phone).withPrimary(true)
                        .withIsPhoneValid(order.getBilling().getIsPhoneValid()).build()) : List.of();

        // Map WooCommerce order status to OrderStatus enum
        OrderStatus orderStatus = null;
        try {
            orderStatus = OrderStatus.fromValue(order.getStatus().toString());
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {} for order ID: {}", order.getStatus(), order.getId(), e);
        }

        // Map WooCommerce payment method to PaymentStatus enum
        PaymentStatus paymentStatus = mapPaymentMethodToPaymentStatus(order.getPaymentMethod());

        var extendedFields = PurchaseItem.builder()
                .withNumOfPurchases(1L)
                // GMT date is used to ensure consistency with other date fields
                .withLastPurchaseDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(order.getDateCreatedGmt()))
                .withTotalSpentAmount(BigDecimal.valueOf(Double.parseDouble(order.getTotal())))
                .withTotalSpentCurrency(order.getCurrency())
                .withOrderStatus(orderStatus)
                .withPaymentStatus(paymentStatus)
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

    private Name buildName(Order order) {
        return Name.builder()
                .withFirst(order.getBilling().getFirstName())
                .withLast(order.getBilling().getLastName())
                .withFull(order.getBilling().getFirstName() + " " + order.getBilling().getLastName())
                .build();
    }

    private PaymentStatus mapPaymentMethodToPaymentStatus(String paymentMethod) {
        if (paymentMethod == null) {
            return PaymentStatus.not_paid;
        }
        return switch (paymentMethod.toLowerCase()) {
            case "phonepe", "phonepe payment solutions", "razorpay" -> PaymentStatus.paid;
            default -> PaymentStatus.not_paid;
        };
    }
}