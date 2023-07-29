package com.glamaya.datacontracts.pricecalculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.commons.validator.SchemaValidator;
import com.glamaya.datacontracts.pricecalculator.model.ComparablePriceObject;
import com.glamaya.datacontracts.pricecalculator.model.PriceCalculatorFactor;

public class Main {
    public static void main(String[] args) {

        /*SchemaValidator sv = new SchemaValidator("/schema/price-object.json",
                "/schema/price-calculator-factor.json", new ObjectMapper());*/

        var sv = new SchemaValidator(new ObjectMapper());
        var priceCalculatorFactor = new PriceCalculatorFactor(90, 40, 60, 150, 180, 60);

        sv.validate(priceCalculatorFactor, "/schema/price-calculator-factor.json");


        PriceCalculator priceCalculator = new PriceCalculatorImpl();

        //dataLoad(priceCalculator);

        priceCalculator.printCPSPAndMRPSet();

        var sp = priceCalculator.calculateSellingPrice(5, 249, 1099, priceCalculatorFactor);
        System.out.println(sp);


        System.out.println(priceCalculator.getCPSPAndMRP(2, false));
        System.out.println(priceCalculator.getCPSPAndMRP(6, false));
        System.out.println(priceCalculator.getCPSPAndMRP(8, false));
        System.out.println(priceCalculator.getCPSPAndMRP(21, false));
        System.out.println(priceCalculator.getCPSPAndMRP(25, false));
        System.out.println(priceCalculator.getCPSPAndMRP(30, false));
        System.out.println(priceCalculator.getCPSPAndMRP(1600, false));

    }

    private static void dataLoad(PriceCalculator priceCalculator) {
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(5, 18, 249, 1099));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(10, 25, 299, 1349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(15, 45, 299, 1349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(20, 45, 299, 1349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(30, 64, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(35, 65, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(40, 77, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(45, 77, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(50, 89, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(60, 135, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(70, 135, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(75, 135, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(80, 142, 349, 1799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(90, 150, 449, 2149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(100, 165, 449, 2149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(110, 173, 449, 2149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(120, 178, 449, 2149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(125, 178, 449, 2149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(130, 197, 449, 2149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(135, 199, 449, 2149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(140, 210, 499, 2349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(150, 245, 499, 2349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(160, 300, 499, 2349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(165, 300, 499, 2349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(180, 300, 499, 2349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(185, 300, 549, 2899));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(190, 300, 599, 2899));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(200, 300, 599, 2899));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(210, 300, 599, 2899));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(220, 300, 599, 2899));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(225, 300, 599, 2899));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(230, 300, 599, 2899));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(249, 430, 649, 3149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(250, 430, 649, 3149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(270, 430, 649, 3149));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(280, 430, 749, 3799));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(290, 550, 749, 3999));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(300, 550, 899, 4349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(340, 550, 899, 4349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(380, 550, 899, 4349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(400, 550, 899, 4349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(450, 550, 899, 5549));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(465, 580, 949, 5549));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(480, 580, 949, 5549));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(500, 580, 1149, 5999));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(530, 1050, 1149, 5999));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(550, 1050, 1149, 5999));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(600, 1050, 1249, 6249));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(700, 1050, 1399, 8049));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(750, 1050, 1399, 8049));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(800, 1100, 1599, 8049));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(1030, 1100, 1599, 8049));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(1140, 1250, 1599, 8049));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(1380, 1450, 2449, 14349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(1430, 1650, 2599, 14349));
        priceCalculator.addCostPriceSellingPriceAndMRP(new ComparablePriceObject(1530, 1680, 2749, 14349));
    }
}