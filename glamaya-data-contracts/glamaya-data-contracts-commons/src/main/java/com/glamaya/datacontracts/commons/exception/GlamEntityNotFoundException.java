package com.glamaya.datacontracts.commons.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = false)
public class GlamEntityNotFoundException extends RuntimeException {

    private String entity;

    public GlamEntityNotFoundException() {
        super(HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    public GlamEntityNotFoundException(String message) {
        super(message);
    }

    public <T> GlamEntityNotFoundException(Class<T> entity) {
        super(HttpStatus.NOT_FOUND.getReasonPhrase());
        this.entity = entity.getSimpleName();
    }

    public <T> GlamEntityNotFoundException(Class<T> entity, String message) {
        super(message);
        this.entity = entity.getSimpleName();
    }
}