package com.glamaya.sync.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EcomModel<T> {

    private String id;
    private T data;
}
