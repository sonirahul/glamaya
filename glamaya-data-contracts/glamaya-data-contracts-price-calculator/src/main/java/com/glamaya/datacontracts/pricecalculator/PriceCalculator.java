package com.glamaya.datacontracts.pricecalculator;

import com.glamaya.datacontracts.pricecalculator.model.ComparablePriceObject;
import com.glamaya.datacontracts.pricecalculator.model.PriceCalculatorFactor;

public interface PriceCalculator {
    ComparablePriceObject getCPSPAndMRP(int cp, boolean findExactMatchOnly);

    void addCostPriceSellingPriceAndMRP(ComparablePriceObject priceObject);

    int calculateSellingPrice(int costPrice, int currentSellingPrice, int mrp, PriceCalculatorFactor request);

    void reset();

    boolean isLoadingRequired();

    void printCPSPAndMRPSet();
}
