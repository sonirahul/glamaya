package com.glamaya.datacontracts.commons.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = false)
public class GlamLockedEntityModificationException extends RuntimeException {

    private final HttpStatus status = HttpStatus.BAD_REQUEST;

    public GlamLockedEntityModificationException() {
        super(HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    public GlamLockedEntityModificationException(String message) {
        super(message);
    }
}