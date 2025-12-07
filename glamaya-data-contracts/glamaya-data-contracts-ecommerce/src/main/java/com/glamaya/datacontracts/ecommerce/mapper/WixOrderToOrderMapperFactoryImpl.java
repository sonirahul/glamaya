package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.BillingAddress;
import com.glamaya.datacontracts.ecommerce.Customer;
import com.glamaya.datacontracts.ecommerce.LineItem;
import com.glamaya.datacontracts.ecommerce.Order;
import com.glamaya.datacontracts.ecommerce.Source;
import com.glamaya.datacontracts.ecommerce.SourceType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WixOrderToOrderMapperFactoryImpl implements OrderMapperFactory<com.glamaya.datacontracts.wix.Order> {

    @Override
    public Order toGlamayaOrder(com.glamaya.datacontracts.wix.Order source, String sourceAccountName) {
        if (source == null) return null;
        var target = Order.builder().build();

        mapSources(source, target, sourceAccountName);
        mapStatusCurrencyAndDates(source, target);
        mapPriceSummary(source, target);
        mapCustomer(source, target);
        mapBillingAddress(source, target);
        mapLineItems(source, target);
        aggregateQuantity(target);
        mapNotesAndMetadata(source, target);

        return target;
    }

    private void mapSources(com.glamaya.datacontracts.wix.Order source, Order target, String sourceAccountName) {
        var sourceObj = Source.builder()
                .withSourceName(sourceAccountName)
                .withSourceType(SourceType.WIX)
                .withSourceId(String.valueOf(source.getId()))
                .withId(String.valueOf(source.getId())).build();
        var sources = List.of(sourceObj);
        target.setSources(sources);

        String uuid = UUID.nameUUIDFromBytes((sourceObj.getSourceName() + "-" + sourceObj.getSourceType()
                + "-" + sourceObj.getSourceId() + "-" + sourceObj.getId()).toUpperCase()
                .getBytes(StandardCharsets.UTF_8)).toString();
        target.setId(uuid);
    }

    private void mapStatusCurrencyAndDates(com.glamaya.datacontracts.wix.Order source, Order target) {
        if (source.getStatus() != null) target.setStatus(String.valueOf(source.getStatus()));
        target.setCurrency(source.getCurrency());
        target.setCreatedAt(source.getCreatedDate());
        target.setUpdatedAt(source.getUpdatedDate());
    }

    private void mapPriceSummary(com.glamaya.datacontracts.wix.Order source, Order target) {
        var ps = source.getPriceSummary();
        if (ps == null) return;
        target.setTotalPrice(extractPrice(ps.getTotalPrice(), ps.getTotal(), ps.getSubtotal()));
        target.setDiscountTotal(extractPrice(ps.getDiscount(), null, null));
        target.setShippingTotal(extractPrice(ps.getShipping(), null, null));
        target.setTaxTotal(extractPrice(ps.getTax(), null, null));
    }

    private void mapCustomer(com.glamaya.datacontracts.wix.Order source, Order target) {
        var buyer = source.getBuyerInfo();
        var billingInfo = source.getBillingInfo();
        var contactDetails = billingInfo != null ? billingInfo.getContactDetails() : null;
        if (buyer == null && contactDetails == null) return;

        var cust = Customer.builder().build();
        if (buyer != null) {
            cust.setId(buyer.getContactId());
            cust.setEmail(buyer.getEmail());
        }
        if (contactDetails != null) {
            cust.setFirstName(contactDetails.getFirstName());
            cust.setLastName(contactDetails.getLastName());
            if (cust.getPhone() == null) cust.setPhone(contactDetails.getPhone());
        }
        target.setCustomer(cust);
    }

    private void mapBillingAddress(com.glamaya.datacontracts.wix.Order source, Order target) {
        var billingInfo = source.getBillingInfo();
        if (billingInfo != null && billingInfo.getAddress() != null) {
            target.setBillingAddress(toBillingAddress(billingInfo.getAddress()));
        }
    }

    private void mapLineItems(com.glamaya.datacontracts.wix.Order source, Order target) {
        if (source.getLineItems() == null) return;
        List<LineItem> items = new ArrayList<>();
        for (var li : source.getLineItems()) {
            if (li == null) continue;
            var uni = LineItem.builder().build();
            uni.setLineId(li.getId());
            if (li.getQuantity() != null) {
                try { uni.setQuantity(li.getQuantity().longValue()); } catch (Exception ignored) {}
            }
            if (li.getProductName() != null) {
                uni.setName(li.getProductName().toString());
            }
            uni.setUnitPrice(extractPrice(li.getPrice(), null, null));
            var totalPrice = extractPrice(li.getTotalPriceAfterTax(), li.getTotalPriceBeforeTax(), null);
            uni.setTotalPrice(totalPrice);
            uni.setDiscountAmount(extractPrice(li.getTotalDiscount(), null, null));
            items.add(uni);
        }
        target.setLineItems(items);
    }

    private void aggregateQuantity(Order target) {
        if (target.getLineItems() == null) return;
        long qty = target.getLineItems().stream().mapToLong(li -> li.getQuantity() == null ? 0L : li.getQuantity()).sum();
        target.setTotalQuantity(qty == 0 ? null : qty);
    }

    private void mapNotesAndMetadata(com.glamaya.datacontracts.wix.Order source, Order target) {
        target.setNotes(source.getBuyerNote());
        if (target.getPlatformMetadata() == null) {
            target.setPlatformMetadata(com.glamaya.datacontracts.ecommerce.PlatformMetadata.builder().build());
        }
    }

    private BillingAddress toBillingAddress(com.glamaya.datacontracts.wix.AddressObject a) {
        if (a == null) return null;
        var b = BillingAddress.builder().build();
        b.setAddress1(a.getAddressLine());
        b.setAddress2(a.getAddressLine2());
        b.setCity(a.getCity());
        b.setState(a.getSubdivision());
        b.setPostalCode(a.getPostalCode());
        b.setCountryCode(a.getCountry());
        return b;
    }

    private java.math.BigDecimal extractPrice(com.glamaya.datacontracts.wix.Price preferred, com.glamaya.datacontracts.wix.Price fallback1, com.glamaya.datacontracts.wix.Price fallback2) {
        com.glamaya.datacontracts.wix.Price p = preferred != null ? preferred : (fallback1 != null ? fallback1 : fallback2);
        if (p == null || p.getAmount() == null) return null;
        try { return new BigDecimal(p.getAmount()); } catch (NumberFormatException e) { return null; }
    }
}
