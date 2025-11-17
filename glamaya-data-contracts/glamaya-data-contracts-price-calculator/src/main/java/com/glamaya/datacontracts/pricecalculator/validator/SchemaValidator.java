package com.glamaya.datacontracts.pricecalculator.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.pricecalculator.model.ComparablePriceObject;
import com.glamaya.datacontracts.pricecalculator.model.PriceCalculatorFactor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
@Slf4j
public class SchemaValidator {

    private final Schema priceObjectSchema;
    private final Schema priceCalculatorFactorSchema;

    private final ObjectMapper objectMapper;

    public SchemaValidator(@Value("${application.validator.price-object-schema}") String priceObjectSchemaPath,
                           @Value("${application.validator.price-calculator-factor-schema}") String priceCalculatorFactorSchemaPath,
                           ObjectMapper objectMapper) {

        this.priceObjectSchema = loadSchema(priceObjectSchemaPath);
        this.priceCalculatorFactorSchema = loadSchema(priceCalculatorFactorSchemaPath);
        this.objectMapper = objectMapper;
    }

    private Schema loadSchema(String schemaPath) {
        var schemaInputStream = this.getClass().getResourceAsStream(schemaPath);
        if (schemaInputStream == null) {
            throw new RuntimeException(String.format("Error loading JSON schema: %s", schemaPath));
        }
        var rawSchema = new JSONObject(new JSONTokener(schemaInputStream));
        return SchemaLoader.load(rawSchema);
    }

    public void validate(Object object) {
        try {
            String message = objectMapper.writeValueAsString(object);
            if (object instanceof ComparablePriceObject) {
                this.priceObjectSchema.validate(new JSONObject(message));
            }
            if (object instanceof PriceCalculatorFactor) {
                this.priceCalculatorFactorSchema.validate(new JSONObject(message));
            }
        } catch (ValidationException e) {

            if (e.getCausingExceptions().isEmpty()) {
                // Only one validation failure
                var mf = MessageFormat.format("Object: {0}, failed schema {1} validation, error message: {2}", object, e.getViolatedSchema(), e.getMessage());

                System.out.println(mf);
            } else {
                // Multiple validation failures
                for (ValidationException failure : e.getCausingExceptions()) {
                    var mf = MessageFormat.format("Object: {0}, failed schema {1} validation, error message: {2}", object, failure.getViolatedSchema(), failure.getMessage());
                    System.out.println(mf);
                    System.out.println("-----");
                }
            }
            throw new RuntimeException("Error validating message", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error reading message", e);
        }
    }

}
