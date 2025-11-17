package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.Address;
import com.glamaya.datacontracts.ecommerce.AddressType;
import com.glamaya.datacontracts.ecommerce.Contact;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface ContactMapperFactory<T> {

    String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    Contact toGlamayaContact(T data, String sourceAccountName);

    default boolean matches(String classType) {
        if (classType == null) {
            return false;
        }
        return this.getClass().getGenericInterfaces()[0].getTypeName().contains(classType);
    }

    default String formatPhoneNumber(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return null;

        // Remove all non-digit and non-plus characters
        String sanitized = phoneNumber.replaceAll("[^\\d+]", "");

        // If starts with '91' and is 12 digits, treat as Indian number
        if (sanitized.matches("^91\\d{10}$")) {
            sanitized = "+" + sanitized;
            countryCode = "IN";
        }
        // If exactly 10 digits, treat as Indian number
        else if (sanitized.matches("^\\d{10}$")) {
            sanitized = "+91" + sanitized;
            countryCode = "IN";
        }

        try {
            Phonenumber.PhoneNumber number = phoneUtil.parse(sanitized, countryCode);
            if (phoneUtil.isValidNumber(number)) {
                return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            }
        } catch (NumberParseException e) {
            // Ignore and return null below
        }
        return null;
    }

    default String formatEmail(String email) {
        if (StringUtils.hasLength(email)) {
            Pattern pattern = Pattern.compile(EMAIL_REGEX);
            Matcher matcher = pattern.matcher(email);
            return matcher.matches() ? email.toLowerCase() : null;
        }
        return null;
    }

    default String findCountryCode(List<Address> addresses) {
        var address = addresses.stream()
                .filter(addr -> AddressType.BILLING.equals(addr.getType()) && "IN".equalsIgnoreCase(addr.getCountry()))
                .findFirst()
                .or(() -> addresses.stream()
                        .filter(addr -> AddressType.SHIPPING.equals(addr.getType()))
                        .findFirst());

        if (address.isPresent()) {
            return address.get().getCountry();
        }
        return "IN";
    }

    default String toUpperCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return Arrays.stream(input.toLowerCase().trim().split("\\s+"))
                .map(org.apache.commons.lang3.StringUtils::capitalize)
                .map(String::trim).collect(Collectors.joining(" "));
    }
}
