//package com.glamaya.datacontracts.ecommerce.mapper;
//
//import com.glamaya.datacontracts.wix.BillingInfo;
//import com.glamaya.datacontracts.wix.BuyerInfo;
//import com.glamaya.datacontracts.wix.PriceSummary;
//import com.glamaya.datacontracts.wix.WixOrder;
//import com.glamaya.datacontracts.woocommerce.Address;
//import com.glamaya.datacontracts.woocommerce.LineItem;
//import com.glamaya.datacontracts.woocommerce.Order;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class OrderMapper {
//
//    public static Order toWoocommerceOrder(com.glamaya.datacontracts.wix.Order wixOrder) {
//        if (wixOrder == null) {
//            return null;
//        }
//
//        Order woocommerceOrder = new Order();
//        woocommerceOrder.setId(wixOrder.getId());
//        woocommerceOrder.setNumber(wixOrder.getNumber());
//        woocommerceOrder.setDateCreated(wixOrder.getCreatedDate());
//        woocommerceOrder.setDateModified(wixOrder.getUpdatedDate());
//        woocommerceOrder.setLineItems(mapLineItems(wixOrder.getLineItems()));
//        woocommerceOrder.setBuyerInfo(mapBuyerInfo(wixOrder.getBuyerInfo()));
//        woocommerceOrder.setPaymentStatus(wixOrder.getPaymentStatus());
//        woocommerceOrder.setFulfillmentStatus(wixOrder.getFulfillmentStatus());
//        woocommerceOrder.setBuyerLanguage(wixOrder.getBuyerLanguage());
//        woocommerceOrder.setWeightUnit(wixOrder.getWeightUnit());
//        woocommerceOrder.setCurrency(wixOrder.getCurrency());
//        woocommerceOrder.setTaxIncludedInPrices(wixOrder.getTaxIncludedInPrices());
//        woocommerceOrder.setSiteLanguage(wixOrder.getSiteLanguage());
//        woocommerceOrder.setPriceSummary(mapPriceSummary(wixOrder.getPriceSummary()));
//        woocommerceOrder.setBilling(mapBillingInfo(wixOrder.getBillingInfo()));
//        woocommerceOrder.setShipping(mapShippingInfo(wixOrder.getShippingInfo()));
//        woocommerceOrder.setStatus(wixOrder.getStatus());
//        woocommerceOrder.setArchived(wixOrder.getArchived());
//        woocommerceOrder.setTaxSummary(mapTaxSummary(wixOrder.getTaxSummary()));
//        woocommerceOrder.setTaxInfo(mapTaxInfo(wixOrder.getTaxInfo()));
//        woocommerceOrder.setAppliedDiscounts(wixOrder.getAppliedDiscounts());
//        woocommerceOrder.setActivities(mapActivities(wixOrder.getActivities()));
//        woocommerceOrder.setAttributionSource(wixOrder.getAttributionSource());
//        woocommerceOrder.setCreatedBy(mapCreatedBy(wixOrder.getCreatedBy()));
//        woocommerceOrder.setChannelInfo(mapChannelInfo(wixOrder.getChannelInfo()));
//        woocommerceOrder.setSeenByAHuman(wixOrder.getSeenByAHuman());
//        woocommerceOrder.setCheckoutId(wixOrder.getCheckoutId());
//        woocommerceOrder.setCustomFields(wixOrder.getCustomFields());
//        woocommerceOrder.setCartId(wixOrder.getCartId());
//        woocommerceOrder.setIsInternalOrderCreate(wixOrder.getIsInternalOrderCreate());
//        woocommerceOrder.setPayNow(mapPayNow(wixOrder.getPayNow()));
//        woocommerceOrder.setBalanceSummary(mapBalanceSummary(wixOrder.getBalanceSummary()));
//        woocommerceOrder.setAdditionalFees(wixOrder.getAdditionalFees());
//        woocommerceOrder.setPurchaseFlowId(wixOrder.getPurchaseFlowId());
//        woocommerceOrder.setRecipientInfo(mapRecipientInfo(wixOrder.getRecipientInfo()));
//        // Add other field mappings as necessary
//
//        return woocommerceOrder;
//    }
//
//    private static Address mapBillingInfo(BillingInfo billingInfo) {
//
//        if (billingInfo == null) {
//            return null;
//        }
//
//        Address address = new Address();
//        address.setFirstName(billingInfo.getContactDetails().getFirstName());
//        address.setLastName(billingInfo.getContactDetails().getLastName());
//        address.setCompany(billingInfo.getCompany());
//        address.setAddress1(billingInfo.getAddress().getAddressLine());
//        address.setAddress2(billingInfo.getAddress2());
//        address.setCity(billingInfo.getAddress().getCity());
//        address.setPostcode(billingInfo.getAddress().getPostalCode());
//        address.setCountry(billingInfo.getCountry());
//        address.setState(billingInfo.getState());
//        address.setEmail(billingInfo.getEmail());
//        address.setPhone(billingInfo.getContactDetails().getPhone());
//        // Add other field mappings as necessary
//
//        return address;
//    }
//
//    public static WixOrder toWixOrder(WoocommerceOrder woocommerceOrder) {
//        if (woocommerceOrder == null) {
//            return null;
//        }
//
//        WixOrder wixOrder = new WixOrder();
//        wixOrder.setId(woocommerceOrder.getId());
//        wixOrder.setNumber(woocommerceOrder.getNumber());
//        wixOrder.setCreatedDate(woocommerceOrder.getCreatedDate());
//        wixOrder.setUpdatedDate(woocommerceOrder.getUpdatedDate());
//        wixOrder.setLineItems(mapWooLineItems(woocommerceOrder.getLineItems()));
//        wixOrder.setBuyerInfo(mapWooBuyerInfo(woocommerceOrder.getBuyerInfo()));
//        wixOrder.setPaymentStatus(woocommerceOrder.getPaymentStatus());
//        wixOrder.setFulfillmentStatus(woocommerceOrder.getFulfillmentStatus());
//        wixOrder.setBuyerLanguage(woocommerceOrder.getBuyerLanguage());
//        wixOrder.setWeightUnit(woocommerceOrder.getWeightUnit());
//        wixOrder.setCurrency(woocommerceOrder.getCurrency());
//        wixOrder.setTaxIncludedInPrices(woocommerceOrder.getTaxIncludedInPrices());
//        wixOrder.setSiteLanguage(woocommerceOrder.getSiteLanguage());
//        wixOrder.setPriceSummary(mapWooPriceSummary(woocommerceOrder.getPriceSummary()));
//        wixOrder.setBillingInfo(mapWooBillingInfo(woocommerceOrder.getBillingInfo()));
//        wixOrder.setShippingInfo(mapWooShippingInfo(woocommerceOrder.getShippingInfo()));
//        wixOrder.setStatus(woocommerceOrder.getStatus());
//        wixOrder.setArchived(woocommerceOrder.getArchived());
//        wixOrder.setTaxSummary(mapWooTaxSummary(woocommerceOrder.getTaxSummary()));
//        wixOrder.setTaxInfo(mapWooTaxInfo(woocommerceOrder.getTaxInfo()));
//        wixOrder.setAppliedDiscounts(woocommerceOrder.getAppliedDiscounts());
//        wixOrder.setActivities(mapWooActivities(woocommerceOrder.getActivities()));
//        wixOrder.setAttributionSource(woocommerceOrder.getAttributionSource());
//        wixOrder.setCreatedBy(mapWooCreatedBy(woocommerceOrder.getCreatedBy()));
//        wixOrder.setChannelInfo(mapWooChannelInfo(woocommerceOrder.getChannelInfo()));
//        wixOrder.setSeenByAHuman(woocommerceOrder.getSeenByAHuman());
//        wixOrder.setCheckoutId(woocommerceOrder.getCheckoutId());
//        wixOrder.setCustomFields(woocommerceOrder.getCustomFields());
//        wixOrder.setCartId(woocommerceOrder.getCartId());
//        wixOrder.setIsInternalOrderCreate(woocommerceOrder.getIsInternalOrderCreate());
//        wixOrder.setPayNow(mapWooPayNow(woocommerceOrder.getPayNow()));
//        wixOrder.setBalanceSummary(mapWooBalanceSummary(woocommerceOrder.getBalanceSummary()));
//        wixOrder.setAdditionalFees(woocommerceOrder.getAdditionalFees());
//        wixOrder.setPurchaseFlowId(woocommerceOrder.getPurchaseFlowId());
//        wixOrder.setRecipientInfo(mapWooRecipientInfo(woocommerceOrder.getRecipientInfo()));
//        // Add other field mappings as necessary
//
//        return wixOrder;
//    }
//
//    private static List<LineItem> mapLineItems(List<com.glamaya.datacontracts.wix.LineItem> wixLineItems) {
//        return wixLineItems.stream().map(wixLineItem -> {
//            LineItem wooLineItem = new LineItem();
//            wooLineItem.setId(wixLineItem.getId());
//            wooLineItem.setName(wixLineItem.getName());
//            wooLineItem.setQuantity(wixLineItem.getQuantity());
//            wooLineItem.setPrice(wixLineItem.getPrice());
//            // Add other field mappings as necessary
//            return wooLineItem;
//        }).collect(Collectors.toList());
//    }
//
//    private static List<com.glamaya.datacontracts.wix.LineItem> mapWooLineItems(List<LineItem> wooLineItems) {
//        return wooLineItems.stream().map(wooLineItem -> {
//            com.glamaya.datacontracts.wix.LineItem wixLineItem = new com.glamaya.datacontracts.wix.LineItem();
//            wixLineItem.setId(wooLineItem.getId());
//            wixLineItem.setName(wooLineItem.getName());
//            wixLineItem.setQuantity(wooLineItem.getQuantity());
//            wixLineItem.setPrice(wooLineItem.getPrice());
//            // Add other field mappings as necessary
//            return wixLineItem;
//        }).collect(Collectors.toList());
//    }
//
//    private static BuyerInfo mapBuyerInfo(com.glamaya.datacontracts.wix.BuyerInfo wixBuyerInfo) {
//        if (wixBuyerInfo == null) {
//            return null;
//        }
//        BuyerInfo wooBuyerInfo = new BuyerInfo();
//        wooBuyerInfo.setName(wixBuyerInfo.getName());
//        wooBuyerInfo.setEmail(wixBuyerInfo.getEmail());
//        wooBuyerInfo.setPhone(wixBuyerInfo.getPhone());
//        // Add other field mappings as necessary
//        return wooBuyerInfo;
//    }
//
//    private static com.glamaya.datacontracts.wix.BuyerInfo mapWooBuyerInfo(BuyerInfo wooBuyerInfo) {
//        if (wooBuyerInfo == null) {
//            return null;
//        }
//        com.glamaya.datacontracts.wix.BuyerInfo wixBuyerInfo = new com.glamaya.datacontracts.wix.BuyerInfo();
//        wixBuyerInfo.setName(wooBuyerInfo.getName());
//        wixBuyerInfo.setEmail(wooBuyerInfo.getEmail());
//        wixBuyerInfo.setPhone(wooBuyerInfo.getPhone());
//        // Add other field mappings as necessary
//        return wixBuyerInfo;
//    }
//
//    private static PriceSummary mapPriceSummary(com.glamaya.datacontracts.wix.PriceSummary wixPriceSummary) {
//        if (wixPriceSummary == null) {
//            return null;
//        }
//        PriceSummary wooPriceSummary = new PriceSummary();
//        wooPriceSummary.setSubtotal(wixPriceSummary.getSubtotal());
//        wooPriceSummary.setShipping(wixPriceSummary.getShipping());
//        wooPriceSummary.setTax(wixPriceSummary.getTax());
//        wooPriceSummary.setDiscount(wixPriceSummary.getDiscount());
//        wooPriceSummary.setTotalPrice(wixPriceSummary.getTotalPrice());
//        // Add other field mappings as necessary
//        return wooPriceSummary;
//    }
//
//    private static com.glamaya.datacontracts.wix.PriceSummary mapWooPriceSummary(PriceSummary wooPriceSummary) {
//        if (wooPriceSummary == null) {
//            return null;
//        }