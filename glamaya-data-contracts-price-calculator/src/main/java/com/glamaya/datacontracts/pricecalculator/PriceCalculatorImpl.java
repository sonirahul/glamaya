package com.glamaya.datacontracts.pricecalculator;

import com.glamaya.datacontracts.pricecalculator.model.ComparablePriceObject;
import com.glamaya.datacontracts.pricecalculator.model.PriceCalculatorFactor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
public class PriceCalculatorImpl implements PriceCalculator {

    private final Set<ComparablePriceObject> cpSPAndMRPSet = new TreeSet<>();

    @Override
    public ComparablePriceObject getCPSPAndMRP(int cp, boolean findExactMatchOnly) {

        ComparablePriceObject previousComparablePriceObject;
        ComparablePriceObject currentObject = null;

        for (ComparablePriceObject priceObject : cpSPAndMRPSet) {
            if (Objects.equals(cp, priceObject.getCostPrice())) {
                return priceObject;
            }
            if (!findExactMatchOnly) {
                previousComparablePriceObject = currentObject;
                currentObject = priceObject;

                if (cp < currentObject.getCostPrice()) {

                    if (Objects.isNull(previousComparablePriceObject)) {
                        return currentObject;
                    }

                    if (cp - previousComparablePriceObject.getCostPrice() > currentObject.getCostPrice() - cp) {
                        return currentObject;
                    } else {
                        return previousComparablePriceObject;
                    }
                }
            }
        }

        return generateComparablePriceObjectIntelligently(currentObject, cp);
    }

    @Override
    public void addCostPriceSellingPriceAndMRP(ComparablePriceObject priceObject) {

        if (cpSPAndMRPSet.isEmpty()) {
            cpSPAndMRPSet.add(priceObject);
        } else {
            ComparablePriceObject maxComparablePriceObjectInSet = cpSPAndMRPSet.stream()
                    .max(Comparator.comparingInt(ComparablePriceObject::getCostPrice)).get();
            int resellingPriceEff;
            if (Objects.equals(priceObject.getCostPrice(), maxComparablePriceObjectInSet.getCostPrice())) {
                resellingPriceEff = priceObject.getResalePrice() < maxComparablePriceObjectInSet.getResalePrice() ? maxComparablePriceObjectInSet.getResalePrice() : (maxComparablePriceObjectInSet.getResalePrice() + priceObject.getResalePrice())/2;
                cpSPAndMRPSet.remove(maxComparablePriceObjectInSet);
            } else {
                resellingPriceEff = Math.max(maxComparablePriceObjectInSet.getResalePrice(), priceObject.getResalePrice());
            }

            cpSPAndMRPSet.add(new ComparablePriceObject(priceObject.getCostPrice(), resellingPriceEff,
                            Math.max(maxComparablePriceObjectInSet.getSellingPrice(), priceObject.getSellingPrice()),
                            Math.max(maxComparablePriceObjectInSet.getMrp(), priceObject.getMrp())));
        }
    }

    @Override
    public int calculateSellingPrice(int costPrice, int currentSellingPrice, int mrp, PriceCalculatorFactor request) {

        var shippingCostToBeAdded = identifyThePartOfShippingCostToBeAddedToProduct(request.getShippingCost(),
                request.getPercentShippingInProductCost());
        int priceWithMinMarginAmount = costPrice + request.getMinMarginAmount();
        int priceWithMaxMarginPercent = (int) (costPrice * ((100 + request.getMaxMarginPercent()) / 100F));

        int priceWithDiscountPercent = 0;
        for (int i = request.getMaxDiscountPercent(); i >= request.getMinDiscountPercent(); i--) {
            priceWithDiscountPercent = mrp - (i * mrp ) / 100;
            if (priceWithDiscountPercent >= currentSellingPrice + shippingCostToBeAdded
                    && priceWithDiscountPercent >= priceWithMinMarginAmount
                    && priceWithDiscountPercent >= priceWithMaxMarginPercent) {
                break;
            }
        }

        int finalPrice = getRoundedOffPrices(priceWithDiscountPercent);

        if (finalPrice < costPrice) {
            throw new RuntimeException("Selling Price can't be less than Cost Price");
        }

        return finalPrice;
    }

    @Override
    public void reset() {
        cpSPAndMRPSet.clear();
    }

    @Override
    public boolean isLoadingRequired() {
        return cpSPAndMRPSet.isEmpty();
    }

    @Override
    public void printCPSPAndMRPSet() {
        cpSPAndMRPSet.forEach(priceObject -> System.out.println(priceObject.toString()));
        System.out.println("Total price entries :" + cpSPAndMRPSet.size());
    }
    
    private ComparablePriceObject generateComparablePriceObjectIntelligently(ComparablePriceObject priceObject, int cp) {

        log.info("Generating Price intelligently for the product");
        if (Objects.isNull(priceObject)) {
            return null;
        }

        var percentHikeInResalePrice = getPercentage(priceObject.getCostPrice(), priceObject.getResalePrice());
        var percentHikeInSellingPrice = getPercentage(priceObject.getCostPrice(), priceObject.getSellingPrice());
        var percentHikeInMRPPrice = getPercentage(priceObject.getCostPrice(), priceObject.getMrp());

        var resellingPrice = increasePriceByPercent(cp, percentHikeInResalePrice);
        var sellingPrice = getRoundedOffPrices(increasePriceByPercent(cp, percentHikeInSellingPrice));
        var mrp = getRoundedOffPrices(increasePriceByPercent(cp, percentHikeInMRPPrice));
        return new ComparablePriceObject(cp, resellingPrice,
                Math.max(sellingPrice, priceObject.getSellingPrice()),
                Math.max(mrp, priceObject.getMrp()));
    }

    private int getPercentage(int num1, int num2) {
        return (num2 * 100) / num1;
    }

    private int increasePriceByPercent(int price, int percentage) {
        return (percentage * price) / 100;
    }

    private int getRoundedOffPrices(int price) {
        int moduloDifference = price % 50;

        int finalPrice;
        if (moduloDifference <= 15) {
            finalPrice = price - moduloDifference - 1;
        } else {
            finalPrice = (50 - moduloDifference) + price - 1;
        }
        return finalPrice;
    }

    private int identifyThePartOfShippingCostToBeAddedToProduct(int shippingCost, int percentShippingInProductCost) {
        shippingCost = (shippingCost * percentShippingInProductCost) / 100;
        // Smaller multiple
        int a = (shippingCost / 10) * 10;

        // Larger multiple
        int b = a + 10;

        // Return of closest of two
        return (shippingCost - a > b - shippingCost) ? b : a;
    }

}
