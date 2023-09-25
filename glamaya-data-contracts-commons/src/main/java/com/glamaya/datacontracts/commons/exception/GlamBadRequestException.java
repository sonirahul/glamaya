package com.glamaya.datacontracts.commons.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = false)
public class GlamBadRequestException extends RuntimeException {

    private final HttpStatus status = HttpStatus.BAD_REQUEST;

    public GlamBadRequestException() {
        super(HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    public GlamBadRequestException(String message) {
        super(message);
    }
}