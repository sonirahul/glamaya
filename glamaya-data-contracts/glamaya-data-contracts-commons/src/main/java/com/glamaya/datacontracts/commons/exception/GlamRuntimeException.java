package com.glamaya.datacontracts.commons.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = false)
public class GlamRuntimeException extends RuntimeException {

    private final HttpStatus status = HttpStatus.BAD_REQUEST;

    public GlamRuntimeException() {
        super(HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    public GlamRuntimeException(String message) {
        super(message);
    }
}