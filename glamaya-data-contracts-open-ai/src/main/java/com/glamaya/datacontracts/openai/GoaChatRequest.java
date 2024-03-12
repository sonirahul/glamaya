package com.glamaya.datacontracts.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class GoaChatRequest {

    @JsonProperty("templates")
    @Field("templates")
    @Valid
    private List<GoaPromptTemplate> templates = new ArrayList<>();

    public abstract Object getEntity();
}
