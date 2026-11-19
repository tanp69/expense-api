package com.example.expenseapi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException exception) {
        ApiError error = new ApiError(
                exception.getStatusCode().value(),
                exception.getReason()
        );

        return ResponseEntity.status(exception.getStatusCode()).body(error);
    }
}