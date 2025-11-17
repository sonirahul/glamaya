package com.glamaya.datacontracts.commons.exception;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {

    private String application;
    private LocalDateTime timestamp;
    private int statusCode;
    private String message;
    private String path;
    public HttpStatus getStatus() {
        this.timestamp = LocalDateTime.now();
        return HttpStatus.valueOf(statusCode);
    }
}