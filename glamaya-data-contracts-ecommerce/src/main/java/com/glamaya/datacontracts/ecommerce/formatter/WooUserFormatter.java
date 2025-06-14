package com.glamaya.datacontracts.ecommerce.formatter;

import com.glamaya.datacontracts.ecommerce.AddressType;
import com.glamaya.datacontracts.woocommerce.Address;
import com.glamaya.datacontracts.woocommerce.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WooUserFormatter implements EcommerceFormatter<User> {

    @Override
    public User format(User user) {

        if (user == null) {
            throw new IllegalArgumentException("Woocommerce user invalid user data received");
        }

        var addressMap = Map.of(
                        AddressType.BILLING, user.getBilling(),
                        AddressType.SHIPPING, user.getShipping()
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

        var email = formatEmail(user.getBilling().getEmail());
        var phone = formatPhoneNumber(Objects.nonNull(user.getBilling()) ?
                user.getBilling().getPhone() : null, findCountryCode(addressMap));

        if (email == null && phone == null) {
            throw new IllegalArgumentException(String.format("Woocommerce user id: %s - email and phone are null", user.getId()));
        }

        if (StringUtils.hasText(phone)) {
            user.getBilling().setPhone(phone);
            user.getBilling().setIsPhoneValid(true);
            user.getShipping().setPhone(phone);
            user.getShipping().setIsPhoneValid(true);
        } else {
            user.getBilling().setIsPhoneValid(false);
            user.getShipping().setIsPhoneValid(false);
        }

        if (StringUtils.hasText(email)) {
            user.getBilling().setIsEmailValid(true);
            user.getBilling().setEmail(email);
            user.getShipping().setIsEmailValid(true);
            user.getShipping().setEmail(email);
        } else {
            user.getBilling().setIsEmailValid(false);
            user.getShipping().setIsEmailValid(false);
        }

        return user;
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
