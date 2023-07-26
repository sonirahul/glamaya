package com.glamaya.datacontracts.openai;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import feign.Param;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.annotation.processing.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
        "version",
        "templates"
})
@Document
@Generated("jsonschema2pojo")
public class GlamayaOpenAiPromptTemplate {

    @JsonProperty("version")
    @Field("version")
    @NotNull
    @Pattern(regexp = "^(\\d+)\\.(\\d+)\\.(\\d+)$")
    private String version;
    
    @JsonProperty("templates")
    @Field("templates")
    @Valid
    @NotNull
    private Map<String, String> templates = new LinkedHashMap<>();
    
    public static GlamayaOpenAiPromptTemplate.GlamayaOpenAiPromptTemplateBuilderBase builder() {
        return new GlamayaOpenAiPromptTemplate.GlamayaOpenAiPromptTemplateBuilder();
    }

    @JsonProperty("version")
    @Param("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("templates")
    @Param("templates")
    @JsonAnyGetter
    public Map<String, String> getTemplates() {
        return this.templates;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, String value) {
        this.templates.put(name, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GlamayaOpenAiPromptTemplate.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("version");
        sb.append('=');
        sb.append(((this.version == null)?"<null>":this.version));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.templates == null)?"<null>":this.templates));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.templates == null)? 0 :this.templates.hashCode()));
        result = ((result* 31)+((this.version == null)? 0 :this.version.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof GlamayaOpenAiPromptTemplate) == false) {
            return false;
        }
        GlamayaOpenAiPromptTemplate rhs = ((GlamayaOpenAiPromptTemplate) other);
        return (((this.templates == rhs.templates)||((this.templates != null)&&this.templates.equals(rhs.templates)))&&((this.version == rhs.version)||((this.version!= null)&&this.version.equals(rhs.version))));
    }

    public static class GlamayaOpenAiPromptTemplateBuilder
            extends GlamayaOpenAiPromptTemplate.GlamayaOpenAiPromptTemplateBuilderBase<GlamayaOpenAiPromptTemplate>
    {


        public GlamayaOpenAiPromptTemplateBuilder() {
            super();
        }

    }

    public static abstract class GlamayaOpenAiPromptTemplateBuilderBase<T extends GlamayaOpenAiPromptTemplate >{

        protected T instance;

        @SuppressWarnings("unchecked")
        public GlamayaOpenAiPromptTemplateBuilderBase() {
            // Skip initialization when called from subclass
            if (this.getClass().equals(GlamayaOpenAiPromptTemplate.GlamayaOpenAiPromptTemplateBuilder.class)) {
                this.instance = ((T) new GlamayaOpenAiPromptTemplate());
            }
        }

        public T build() {
            T result;
            result = this.instance;
            this.instance = null;
            return result;
        }

        public GlamayaOpenAiPromptTemplate.GlamayaOpenAiPromptTemplateBuilderBase withVersion(String version) {
            ((GlamayaOpenAiPromptTemplate) this.instance).version = version;
            return this;
        }

        public GlamayaOpenAiPromptTemplate.GlamayaOpenAiPromptTemplateBuilderBase withAdditionalProperty(String name, String value) {
            ((GlamayaOpenAiPromptTemplate) this.instance).templates.put(name, value);
            return this;
        }

    }

}
