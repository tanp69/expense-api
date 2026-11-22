package com.example.expenseapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException exception) {
        ApiError error = new ApiError(
                exception.getStatusCode().value(),
                exception.getReason()
        );

        log.debug(
                "Request rejected status={} reason={}",
                exception.getStatusCode().value(),
                exception.getReason()
        );

        return ResponseEntity.status(exception.getStatusCode()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception) {

        List<ApiFieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiFieldError(
                        error.getField(),
                        Objects.requireNonNullElse(
                                error.getDefaultMessage(),
                                "Invalid value"
                        )
                ))
                .sorted(Comparator.comparing(ApiFieldError::field)
                        .thenComparing(ApiFieldError::message))
                .toList();

        String message = fieldErrors.stream()
                .map(ApiFieldError::message)
                .findFirst()
                .orElse("Invalid request");

        log.debug("Request validation failed fieldCount={}", fieldErrors.size());

        return ResponseEntity.badRequest()
                .body(new ApiError(400, message, fieldErrors));
    }
}