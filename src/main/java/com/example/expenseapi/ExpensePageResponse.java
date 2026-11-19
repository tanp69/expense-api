package com.example.expenseapi;

import java.util.List;

public record ExpensePageResponse(
        List<ExpenseResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}