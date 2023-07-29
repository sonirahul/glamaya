package com.glamaya.datacontracts.commons.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.MessageFormat;

@Slf4j
@RequiredArgsConstructor
public class SchemaValidator {

    private final ObjectMapper objectMapper;

    private Schema getSchema(String schemaPath) {
        var schemaInputStream = this.getClass().getResourceAsStream(schemaPath);
        if (schemaInputStream == null) {
            throw new RuntimeException(String.format("Error loading JSON schema: %s", schemaPath));
        }
        var rawSchema = new JSONObject(new JSONTokener(schemaInputStream));
        return SchemaLoader.load(rawSchema);
    }

    public void validate(Object object, String schemaPath) {
        try {
            String message = objectMapper.writeValueAsString(object);
            getSchema(schemaPath).validate(new JSONObject(message));
        } catch (ValidationException e) {

            if (e.getCausingExceptions().isEmpty()) {
                // Only one validation failure
                var mf = MessageFormat.format("Object: {0}, error message: {1}", object, e.getMessage());

                log.error(mf);
            } else {
                // Multiple validation failures
                for (ValidationException failure : e.getCausingExceptions()) {
                    var mf = MessageFormat.format("Object: {0}, error message: {1}", object, failure.getMessage());
                    log.error(mf);
                    log.error("-----");
                }
            }
            throw new RuntimeException("Error validating message", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error reading message", e);
        }
    }

}
