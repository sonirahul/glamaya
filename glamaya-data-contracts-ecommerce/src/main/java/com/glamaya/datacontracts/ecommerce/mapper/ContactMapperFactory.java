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

    default String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        String rawNumber = phoneNumber.replaceAll("[\\s\\-()]|^\\+91|^0", "");
        return rawNumber.length() == 10 ? rawNumber : null;
    }

    default String formatPhoneNumber(String phoneNumber, String countryCode) {

        phoneNumber = formatPhoneNumber(phoneNumber);
        if (phoneNumber == null) return null;
        try {
            Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, countryCode);
            return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            return phoneNumber;
        }
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
