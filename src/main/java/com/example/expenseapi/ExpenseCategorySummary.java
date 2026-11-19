package com.example.expenseapi;

import java.math.BigDecimal;

public record ExpenseCategorySummary(
        String category,
        Long expenseCount,
        BigDecimal totalAmount
) {}