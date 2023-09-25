package com.glamaya.datacontracts.commons.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Configuration
public class ExceptionConfiguration {

    @ControllerAdvice
    @Slf4j
    private static class GlamGlobalExceptionHandler extends ResponseEntityExceptionHandler {

        private final String applicationName;

        public GlamGlobalExceptionHandler(@Value("${application.name}") String applicationName) {
            this.applicationName = applicationName;
        }

        @ExceptionHandler({GlamEntityNotFoundException.class, FeignException.NotFound.class})
        public ResponseEntity<ErrorResponse> handleEntityNotFoundException(Exception ex, WebRequest request) {
            return buildErrorResponseResponseEntity(HttpStatus.NOT_FOUND, ex, request);
        }

        @ExceptionHandler(GlamBadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequestException(GlamBadRequestException ex,
                                                                       WebRequest request) {
            return buildErrorResponseResponseEntity(HttpStatus.BAD_REQUEST, ex, request);
        }

        @ExceptionHandler(GlamLockedEntityModificationException.class)
        public ResponseEntity<ErrorResponse> handleLockedEntityModificationException(
                GlamLockedEntityModificationException ex, WebRequest request) {
            return buildErrorResponseResponseEntity(HttpStatus.BAD_REQUEST, ex, request);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, WebRequest request) {
            return buildErrorResponseResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        }

        private ResponseEntity<ErrorResponse> buildErrorResponseResponseEntity(HttpStatus status, Exception ex,
                                                                               WebRequest request) {
            log.error("Error occurred", ex);
            var errorResponse = ErrorResponse.builder()
                    .application(applicationName).statusCode(status.value())
                    .message(ex.getMessage()).path(getRequestPath(request)).build();
            return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
        }

        private String getRequestPath(WebRequest request) {
            if (request instanceof ServletWebRequest) {
                return ((ServletWebRequest) request).getRequest().getRequestURI();
            }
            return "";
        }
    }

}
