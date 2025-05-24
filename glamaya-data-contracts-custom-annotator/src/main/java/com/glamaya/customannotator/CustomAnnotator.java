package com.glamaya.customannotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.annotations.SerializedName;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import feign.Param;
import org.jsonschema2pojo.AbstractAnnotator;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

public class CustomAnnotator extends AbstractAnnotator {

    private final List<String> primaryKeyFields;

    public CustomAnnotator() {
        primaryKeyFields = new ArrayList<>();
        primaryKeyFields.add("id");
    }

    @Override
    public void propertyField(JFieldVar field, JDefinedClass clazz, String propertyName, JsonNode propertyNode) {
        super.propertyField(field, clazz, propertyName, propertyNode);
        if (primaryKeyFields.contains(propertyName)) {
            field.annotate(Id.class);
        } else {
            field.annotate(Field.class).param("value", propertyName);
            field.annotate(SerializedName.class).param("value", propertyName);
        }
        if (propertyNode.has("unique") && propertyNode.get("unique").asBoolean()) {
            field.annotate(Indexed.class).param("unique", true);
        }
    }

    @Override
    public void propertyGetter(JMethod getter, JDefinedClass clazz, String propertyName) {
        super.propertyGetter(getter, clazz, propertyName);
        getter.annotate(Param.class).param("value", propertyName);
    }

    @Override
    public void propertyOrder(JDefinedClass clazz, JsonNode propertyNode) {
        clazz.annotate(Document.class);
    }
}