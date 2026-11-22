package com.example.expenseapi;

import java.util.List;

public record ApiError(
        int status,
        String message,
        List<ApiFieldError> fieldErrors
) {
    public ApiError(int status, String message) {
        this(status, message, List.of());
    }
}