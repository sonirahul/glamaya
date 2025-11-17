package com.glamaya.datacontracts.pricecalculator.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ComparablePriceObject extends PriceObject implements Comparable<ComparablePriceObject> {

    public ComparablePriceObject(Integer costPrice, Integer resalePrice, Integer sellingPrice, Integer mrp) {
        super(costPrice, resalePrice, sellingPrice, mrp);
        setDiscountPercent(BigDecimal.valueOf(100 - (float) getSellingPrice() / getMrp() * 100)
                .setScale(2, RoundingMode.CEILING));
    }

    @Override
    public int compareTo(ComparablePriceObject o) {
        return Long.compare(this.getCostPrice(), o.getCostPrice());
    }
}
