package com.glamaya.datacontracts.ecommerce.formatter;

import com.glamaya.datacontracts.ecommerce.AddressType;
import com.glamaya.datacontracts.woocommerce.Address;
import com.glamaya.datacontracts.woocommerce.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WooOrderFormatter implements EcommerceFormatter<Order> {

    @Override
    public Order format(Order order) {

        if (order == null) {
            throw new IllegalArgumentException("Woocommerce order invalid order data received");
        }

        var addressMap = java.util.Map.of(
                        AddressType.BILLING, order.getBilling(),
                        AddressType.SHIPPING, order.getShipping()
                )
                .entrySet().stream()
                .map(entry -> {
                    Address address = entry.getValue();
                    address.setFirstName(toUpperCamelCase(address.getFirstName()));
                    address.setLastName(toUpperCamelCase(address.getLastName()));
                    address.setCity(toUpperCamelCase(address.getCity()));
                    address.setState(toUpperCamelCase(address.getState()));
                    return Map.entry(entry.getKey(), address);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var email = formatEmail(order.getBilling().getEmail());
        var phone = formatPhoneNumber(Objects.nonNull(order.getBilling()) ?
                order.getBilling().getPhone() : null, findCountryCode(addressMap));

        if (email == null && phone == null) {
            throw new IllegalArgumentException(String.format("Woocommerce order id: %d - email and phone are null", order.getId()));
        }

        if (StringUtils.hasText(phone)) {
            order.getBilling().setPhone(phone);
            order.getBilling().setIsPhoneValid(true);
            order.getShipping().setPhone(phone);
            order.getShipping().setIsPhoneValid(true);
        } else {
            order.getBilling().setIsPhoneValid(false);
            order.getShipping().setPhone(order.getBilling().getPhone());
            order.getShipping().setIsPhoneValid(false);
        }

        if (StringUtils.hasText(email)) {
            order.getBilling().setIsEmailValid(true);
            order.getBilling().setEmail(email);
            order.getShipping().setIsEmailValid(true);
            order.getShipping().setEmail(email);
        } else {
            order.getBilling().setIsEmailValid(false);
            order.getShipping().setEmail(order.getBilling().getEmail());
            order.getShipping().setIsEmailValid(false);
        }

        return order;
    }

    String findCountryCode(Map<AddressType, Address> addressMap) {
        Address billing = addressMap.get(AddressType.BILLING);
        if (billing != null && StringUtils.hasText(billing.getCountry())) {
            return billing.getCountry();
        }
        Address shipping = addressMap.get(AddressType.SHIPPING);
        if (shipping != null && StringUtils.hasText(shipping.getCountry())) {
            return shipping.getCountry();
        }
        return "IN";
    }
}
