package com.example.expenseapi;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:sqlserver://127.0.0.1:1433;"
                + "databaseName=expense_test_db;encrypt=true;trustServerCertificate=true"
)
@Transactional
class ExpenseRepositoryIT {

    @Autowired
    private ExpenseRepository repository;

    @Test
    void findsAmountsWithinInclusiveRangeOrderedById() {
        repository.saveAllAndFlush(List.of(
                new ExpenseEntity(10000L, "food"),
                new ExpenseEntity(50000L, "travel"),
                new ExpenseEntity(20000L, "food"),
                new ExpenseEntity(60000L, "travel")
        ));

        List<Long> amounts = repository
                .findByAmountBetweenOrderByIdAsc(20000L, 50000L)
                .stream()
                .map(ExpenseEntity::getAmount)
                .toList();

        assertEquals(List.of(50000L, 20000L), amounts);
    }
}