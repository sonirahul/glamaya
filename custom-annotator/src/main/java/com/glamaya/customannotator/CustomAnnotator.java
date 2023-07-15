package com.glamaya.customannotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import org.jsonschema2pojo.AbstractAnnotator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

public class CustomAnnotator extends AbstractAnnotator {
    List<String> primaryKeyFields;

    public CustomAnnotator() {
        primaryKeyFields = new ArrayList<>();
        primaryKeyFields.add("_id");
    }

    @Override
    public void propertyField(JFieldVar field, JDefinedClass clazz, String propertyName, JsonNode propertyNode) {
        super.propertyField(field, clazz, propertyName, propertyNode);
        if (primaryKeyFields.contains(propertyName)) {
            field.annotate(Id.class);
        }
        field.annotate(Field.class).param("value", propertyName);
    }

    @Override
    public void propertyOrder(JDefinedClass clazz, JsonNode propertyNode) {
        clazz.annotate(Document.class);
    }
}