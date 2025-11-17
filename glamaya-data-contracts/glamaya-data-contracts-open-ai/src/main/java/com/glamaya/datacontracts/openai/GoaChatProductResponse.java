package com.glamaya.datacontracts.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.glamaya.datacontracts.woocommerce.Product;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(Include.NON_EMPTY)
@JsonPropertyOrder({"entity", "templates"})
@Builder
@Data
public class GoaChatProductResponse {
    @JsonProperty("entity")
    @Field("entity")
    private Product entity;

    @JsonProperty("templates")
    @Field("templates")
    private @Valid List<GoaPromptTemplate> templates = new ArrayList<>();
}
