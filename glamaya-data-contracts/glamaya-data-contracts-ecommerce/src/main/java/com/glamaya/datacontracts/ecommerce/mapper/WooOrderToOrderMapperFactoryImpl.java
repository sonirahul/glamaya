package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.BillingAddress;
import com.glamaya.datacontracts.ecommerce.Customer;
import com.glamaya.datacontracts.ecommerce.Fulfillment;
import com.glamaya.datacontracts.ecommerce.LineItem;
import com.glamaya.datacontracts.ecommerce.Order;
import com.glamaya.datacontracts.ecommerce.Payment;
import com.glamaya.datacontracts.ecommerce.ShippingAddress;
import com.glamaya.datacontracts.ecommerce.Source;
import com.glamaya.datacontracts.ecommerce.SourceType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

@Service
public class WooOrderToOrderMapperFactoryImpl implements OrderMapperFactory<com.glamaya.datacontracts.woocommerce.Order> {

    @Override
    public Order toGlamayaOrder(com.glamaya.datacontracts.woocommerce.Order source, String sourceAccountName) {
        if (source == null) return null;

        var target = Order.builder().build();

        mapSources(source, target, sourceAccountName);
        mapStatusAndMonetary(source, target);
        mapCurrency(source, target);
        mapDates(source, target);
        mapPayment(source, target);
        mapFulfillment(target);
        mapCustomerAndAddresses(source, target);
        mapLineItems(source, target);
        aggregateQuantity(target);
        setNotesAndMetadata(source, target);



        return target;
    }

    private void mapSources(com.glamaya.datacontracts.woocommerce.Order source, Order target, String sourceAccountName) {
        var sourceObj = Source.builder()
                .withSourceName(sourceAccountName)
                .withSourceType(SourceType.WOOCOMMERCE_ORDER)
                .withSourceId(String.valueOf(source.getId()))
                .withId(String.valueOf(source.getId())).build();
        var sources = List.of(sourceObj);
        target.setSources(sources);

        String uuid = UUID.nameUUIDFromBytes((sourceObj.getSourceName() + "-" + sourceObj.getSourceType()
                + "-" + sourceObj.getSourceId() + "-" + sourceObj.getId()).toUpperCase()
                .getBytes(StandardCharsets.UTF_8)).toString();
        target.setId(uuid);
    }

    private void mapStatusAndMonetary(com.glamaya.datacontracts.woocommerce.Order source, Order target) {
        if (source.getStatus() != null) target.setStatus(String.valueOf(source.getStatus()));
        target.setTotalPrice(parseBigDecimal(source.getTotal()));
        target.setDiscountTotal(parseBigDecimal(source.getDiscountTotal()));
        target.setShippingTotal(parseBigDecimal(source.getShippingTotal()));
        target.setTaxTotal(parseBigDecimal(source.getTotalTax()));
    }

    private void mapCurrency(com.glamaya.datacontracts.woocommerce.Order source, Order target) {
        target.setCurrency(source.getCurrency());
    }

    private void mapDates(com.glamaya.datacontracts.woocommerce.Order source, Order target) {
        target.setCreatedAt(parseInstantPref(source.getDateCreatedGmt(), source.getDateCreated()));
        target.setUpdatedAt(parseInstantPref(source.getDateModifiedGmt(), source.getDateModified()));
    }

    private void mapPayment(com.glamaya.datacontracts.woocommerce.Order source, Order target) {
        var payment = Payment.builder().build();
        payment.setMethod(source.getPaymentMethod());
        payment.setStatus(source.getPaymentMethodTitle());
        payment.setTransactionId(source.getTransactionId());
        payment.setPaidAt(parseInstantPref(source.getDatePaidGmt(), source.getDatePaid()));
        target.setPayment(payment);
    }

    private void mapFulfillment(Order target) {
        var fulfillment = Fulfillment.builder().build();
        fulfillment.setStatus(target.getStatus());
        target.setFulfillment(fulfillment);
    }

    private void mapCustomerAndAddresses(com.glamaya.datacontracts.woocommerce.Order source, Order target) {
        if (source.getBilling() != null) {
            var cust = Customer.builder().build();
            cust.setId(source.getCustomerId());
            cust.setEmail(source.getBilling().getEmail());
            cust.setFirstName(source.getBilling().getFirstName());
            cust.setLastName(source.getBilling().getLastName());
            cust.setPhone(source.getBilling().getPhone());
            target.setCustomer(cust);
            target.setBillingAddress(toBillingAddress(source.getBilling()));
        }
        if (source.getShipping() != null) {
            target.setShippingAddress(toShippingAddress(source.getShipping()));
        }
    }

    private void mapLineItems(com.glamaya.datacontracts.woocommerce.Order source, Order target) {
        if (source.getLineItems() == null) {
            return;
        }
        List<LineItem> items = new ArrayList<>();
        for (var li : source.getLineItems()) {
            if (li == null) continue;
            var uni = LineItem.builder().build();
            uni.setLineId(li.getId() == null ? null : String.valueOf(li.getId()));
            uni.setProductId(li.getProductId() == null ? null : String.valueOf(li.getProductId()));
            uni.setVariantId(li.getVariationId() == null ? null : String.valueOf(li.getVariationId()));
            uni.setSku(li.getSku());
            uni.setName(li.getName());
            uni.setQuantity(li.getQuantity());
            uni.setUnitPrice(longToBigDecimal(li.getPrice()));
            uni.setTotalPrice(parseBigDecimal(li.getTotal()));
            uni.setTaxAmount(parseBigDecimal(li.getTotalTax()));

            BigDecimal subtotal = parseBigDecimal(li.getSubtotal());
            BigDecimal total = parseBigDecimal(li.getTotal());
            if (subtotal != null && total != null && subtotal.compareTo(total) > 0) {
                uni.setDiscountAmount(subtotal.subtract(total));
            }
            items.add(uni);
        }
        target.setLineItems(items);
    }

    private void aggregateQuantity(Order target) {
        if (target.getLineItems() == null) return;
        long qty = target.getLineItems().stream().mapToLong(li -> li.getQuantity() == null ? 0L : li.getQuantity()).sum();
        target.setTotalQuantity(qty == 0 ? null : qty);
    }

    private void setNotesAndMetadata(com.glamaya.datacontracts.woocommerce.Order source, Order target) {
        target.setNotes(source.getCustomerNote());
        if (target.getPlatformMetadata() == null) {
            target.setPlatformMetadata(com.glamaya.datacontracts.ecommerce.PlatformMetadata.builder().build());
        }
    }

    private BillingAddress toBillingAddress(com.glamaya.datacontracts.woocommerce.Address a) {
        if (a == null) return null;
        var b = BillingAddress.builder().build();
        b.setFirstName(a.getFirstName());
        b.setLastName(a.getLastName());
        b.setAddress1(a.getAddress1());
        b.setAddress2(a.getAddress2());
        b.setCity(a.getCity());
        b.setState(a.getState());
        b.setPostalCode(a.getPostcode());
        b.setCountryCode(a.getCountry());
        b.setPhone(a.getPhone());
        return b;
    }

    private ShippingAddress toShippingAddress(com.glamaya.datacontracts.woocommerce.Address a) {
        if (a == null) return null;
        var s = ShippingAddress.builder().build();
        s.setFirstName(a.getFirstName());
        s.setLastName(a.getLastName());
        s.setAddress1(a.getAddress1());
        s.setAddress2(a.getAddress2());
        s.setCity(a.getCity());
        s.setState(a.getState());
        s.setPostalCode(a.getPostcode());
        s.setCountryCode(a.getCountry());
        s.setPhone(a.getPhone());
        return s;
    }

    private static BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal longToBigDecimal(Long l) {
        return l == null ? null : BigDecimal.valueOf(l);
    }

    private static java.time.Instant parseInstantPref(String gmt, String local) {
        String iso = (gmt != null && !gmt.isBlank()) ? gmt : local;
        return iso == null || iso.isBlank() ? null : STRING_DATE_TO_INSTANT_FUNCTION.apply(iso);
    }
}
