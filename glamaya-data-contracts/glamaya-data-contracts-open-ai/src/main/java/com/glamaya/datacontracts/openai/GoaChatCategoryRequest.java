package com.glamaya.datacontracts.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.glamaya.datacontracts.woocommerce.Category;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@JsonInclude(Include.NON_EMPTY)
@JsonPropertyOrder({"entity", "templates"})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GoaChatCategoryRequest extends GoaChatRequest {

    @JsonProperty("entity")
    @Field("entity")
    private Category entity;

    @Builder
    public GoaChatCategoryRequest(Category entity, List<GoaPromptTemplate> templates) {
        super(templates);
        this.entity = entity;
    }
}


