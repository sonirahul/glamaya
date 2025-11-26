package com.glamaya.datacontracts.ecommerce.mapper;

import com.glamaya.datacontracts.ecommerce.BillingAddress;
import com.glamaya.datacontracts.ecommerce.Customer;
import com.glamaya.datacontracts.ecommerce.LineItem;
import com.glamaya.datacontracts.ecommerce.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class WixOrderToEcommerceOrderMapperFactoryImpl implements OrderMapperFactory<com.glamaya.datacontracts.wix.Order> {

    @Override
    public Order toGlamayaOrder(com.glamaya.datacontracts.wix.Order source) {

        if (source == null) return null;
        var target = Order.builder().build();
        // Platform identity
        target.setPlatformType("WIX");
        target.setOriginalPlatformId(source.getId());
        target.setOriginalPlatformNumber(source.getNumber());
        if (source.getStatus() != null) target.setStatus(String.valueOf(source.getStatus()));
        target.setCurrency(source.getCurrency());
        target.setCreatedAt(source.getCreatedDate());
        target.setUpdatedAt(source.getUpdatedDate());
        if (source.getPriceSummary() != null) {
            var ps = source.getPriceSummary();
            target.setTotalPrice(extractPrice(ps.getTotalPrice(), ps.getTotal(), ps.getSubtotal()));
            target.setDiscountTotal(extractPrice(ps.getDiscount(), null, null));
            target.setShippingTotal(extractPrice(ps.getShipping(), null, null));
            target.setTaxTotal(extractPrice(ps.getTax(), null, null));
        }
        var buyer = source.getBuyerInfo();
        var billingInfo = source.getBillingInfo();
        var contactDetails = billingInfo != null ? billingInfo.getContactDetails() : null;
        if (buyer != null || contactDetails != null) {
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
        if (billingInfo != null && billingInfo.getAddress() != null) {
            target.setBillingAddress(toBillingAddress(billingInfo.getAddress()));
        }
        if (source.getLineItems() != null) {
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
        if (target.getLineItems() != null) {
            long qty = target.getLineItems().stream().mapToLong(li -> li.getQuantity() == null ? 0L : li.getQuantity()).sum();
            target.setTotalQuantity(qty == 0 ? null : qty);
        }
        target.setNotes(source.getBuyerNote());
        if (target.getPlatformMetadata() == null) {
            target.setPlatformMetadata(com.glamaya.datacontracts.ecommerce.PlatformMetadata.builder().build());
        }
        return target;
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

