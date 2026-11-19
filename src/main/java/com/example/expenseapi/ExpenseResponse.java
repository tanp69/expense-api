package com.example.expenseapi;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        long amount,
        String category,
        LocalDate expenseDate
) {
    public static ExpenseResponse from(ExpenseEntity expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseDate()
        );
    }
}