package com.example.expenseapi;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/expenses/{id}")
    public ExpenseResponse getExpenseById(@PathVariable("id") long id) {
        ExpenseEntity expense = expenseService.getExpenseById(id);
        return ExpenseResponse.from(expense);
    }

    @PutMapping("/expenses/{id}")
    public ExpenseEntity updateExpense(
            @PathVariable("id") long id,
            @RequestBody UpdateExpenseRequest  request) {
        return expenseService.updateExpense(id, request.amount(), request.category());
    }

    @DeleteMapping("/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable("id") long id) {
        expenseService.deleteExpense(id);
    }

    @GetMapping("/expenses/page")
    public ExpensePageResponse getExpensePage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "minAmount", required = false) Long minAmount,
            @RequestParam(name = "maxAmount", required = false) Long maxAmount,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "direction", defaultValue = "asc") String direction) {
        Page<ExpenseResponse> result = expenseService
                .findExpensePage(page, size, minAmount, maxAmount, category, sortBy, direction)
                .map(ExpenseResponse::from);

        return new ExpensePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @GetMapping("/expenses")
    public List<ExpenseResponse> getExpenses(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minAmount", required = false) Long minAmount,
            @RequestParam(name = "maxAmount", required = false) Long maxAmount) {
        return expenseService.findExpenses(category, minAmount, maxAmount)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseEntity createExpense(@RequestBody CreateExpenseRequest request) {
        return expenseService.createExpense(
                request.amount(),
                request.category(),
                request.expenseDate()
        );
    }

    private void validateExpenseRequest(CreateExpenseRequest request) {
        if (request.amount() == null || request.amount() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        if (request.category() == null || request.category().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Category is required");
        }
    }

    @GetMapping("/expenses/summary")
    public List<ExpenseCategorySummary> getExpenseSummary(
            @RequestParam(name = "minTotal", required = false) Long minTotal,
            @RequestParam(name = "month", required = false) YearMonth month) {
        return expenseService.summarizeByCategory(minTotal, month);
    }

    public record CreateExpenseRequest(
            Long amount,
            String category,
            LocalDate expenseDate
    ) {}

    @PatchMapping("/expenses/{id}/date")
    public ExpenseResponse updateExpenseDate(
            @PathVariable("id") long id,
            @RequestBody UpdateExpenseDateRequest request) {
        return ExpenseResponse.from(
                expenseService.updateExpenseDate(id, request.expenseDate())
        );
    }

    public record UpdateExpenseDateRequest(LocalDate expenseDate) {}

    public record UpdateExpenseRequest(Long amount, String category) {}
}