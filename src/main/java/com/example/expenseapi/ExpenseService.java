package com.example.expenseapi;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;

    private static final Logger log =
            LoggerFactory.getLogger(ExpenseService.class);

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }
    public ExpenseEntity getExpenseById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Expense not found"));
    }

    @Transactional
    public void deleteExpense(long id) {
        int deletedRows = repository.deleteExpenseById(id);

        if (deletedRows == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Expense not found");
        }
    }

    public ExpenseEntity createExpense(
            Long amount,
            String category,
            LocalDate expenseDate) {

        validateExpense(amount, category);
        validateExpenseDate(expenseDate);

        ExpenseEntity savedExpense = repository.save(
                new ExpenseEntity(amount, category, expenseDate)
        );

        log.info("Saved expense id={}", savedExpense.getId());

        return savedExpense;
    }


    @Transactional
    public ExpenseEntity updateExpense(long id, Long amount, String category) {
        validateExpense(amount, category);
        ExpenseEntity expense = getExpenseById(id);
        expense.update(amount, category);
        return expense;
    }

    private void validateExpense(Long amount, String category) {
        if (amount == null || amount <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Category is required");
        }
    }



    @Transactional
    public ExpenseEntity updateExpenseDate(long id, LocalDate expenseDate) {
        if (expenseDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Expense date is required");
        }

        validateExpenseDate(expenseDate);

        ExpenseEntity expense = getExpenseById(id);
        expense.changeExpenseDate(expenseDate);
        return expense;
    }

    private void validateExpenseDate(LocalDate expenseDate) {
        if (expenseDate != null && (expenseDate.getYear() < 1 || expenseDate.getYear() > 9999)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Expense date is out of range");
        }
    }

    public List<ExpenseEntity> findExpenses(
            String category, Long minAmount, Long maxAmount) {
        long min = minAmount == null ? 0L : minAmount;
        long max = maxAmount == null ? Long.MAX_VALUE : maxAmount;

        validateAmountRange(min, max);

        List<ExpenseEntity> expenses =
                repository.findByAmountBetweenOrderByIdAsc(min, max);

        if (category == null) {
            return expenses;
        }

        List<ExpenseEntity> filteredExpenses = new ArrayList<>();
        for (ExpenseEntity expense : expenses) {
            if (category.equals(expense.getCategory())) {
                filteredExpenses.add(expense);
            }
        }

        return filteredExpenses;
    }

    private void validateAmountRange(long min, long max) {
        if (min < 0 || max < 0 || min > max) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid amount range");
        }
    }

    public Page<ExpenseEntity> findExpensePage(
            int page, int size, Long minAmount, Long maxAmount,
            String category, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100");
        }

        long min = minAmount == null ? 0L : minAmount;
        long max = maxAmount == null ? Long.MAX_VALUE : maxAmount;
        validateAmountRange(min, max);

        if (!sortBy.equals("id") && !sortBy.equals("amount")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "sortBy must be id or amount");
        }

        Sort.Direction sortDirection = switch (direction) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "direction must be asc or desc");
        };

        Sort sort = Sort.by(sortDirection, sortBy);
        if (sortBy.equals("amount")) {
            sort = sort.and(Sort.by("id"));
        }


        PageRequest request = PageRequest.of(page, size, sort);
        if (category == null) {
            return repository.findByAmountBetween(min, max, request);
        }

        return repository.findByCategoryAndAmountBetween(category, min, max, request);
    }

    public List<ExpenseCategorySummary> summarizeByCategory(Long minTotal, YearMonth month) {
        if (month != null && (month.getYear() < 1 || month.getYear() > 9999)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month is out of range");
        }

        LocalDate monthStart = month == null ? null : month.atDay(1);
        LocalDate monthEnd = month == null ? null : month.atEndOfMonth();

        return repository.summarizeByCategory(minTotal, monthStart, monthEnd);
    }
}