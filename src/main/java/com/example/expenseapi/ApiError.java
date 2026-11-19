package com.example.expenseapi;

public record ApiError(int status, String message) {}