package com.example.expenseapi;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
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