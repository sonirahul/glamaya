/*
package com.glamaya.customannotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.rules.SchemaRule;


import java.util.Iterator;

public class JsonSchemaRuleFactory extends RuleFactory {

    @Override
    public Rule<JClassContainer, JType> getSchemaRule() {
        return new MySchemaRule(this);
    }

    private class MySchemaRule extends SchemaRule {

        public MySchemaRule(JsonSchemaRuleFactory jsonSchemaRuleFactory) {
            super(jsonSchemaRuleFactory);
        }

        @Override
        public JType apply(String nodeName, JsonNode schemaNode, JsonNode parent, JClassContainer generatableType, Schema schema) {
            final JType apply = super.apply(nodeName, schemaNode, parent, generatableType, schema);

            final JsonNode definitions = schemaNode.get("definitions");
            if (definitions != null && definitions.isObject()) {
                ObjectNode objectNode = (ObjectNode) definitions;
                final Iterator<String> nodeIterator = objectNode.fieldNames();
                while (nodeIterator.hasNext()) {
                    final String name = nodeIterator.next();
                    try {
                        final ObjectNode node = (ObjectNode) objectNode.get(name);
                        final Schema currentSchema = getSchemaStore().create(schema, "#/definitions/" + name, getGenerationConfig().getRefFragmentPathDelimiters());
                        getSchemaRule().apply(name, node, schemaNode, generatableType.getPackage(), currentSchema);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return apply;
        }
    }
}
*/
