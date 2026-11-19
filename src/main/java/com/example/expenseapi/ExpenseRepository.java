package com.example.expenseapi;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {
    @Modifying
    @Query("delete from ExpenseEntity e where e.id = :id")
    int deleteExpenseById(@Param("id") long id);
    List<ExpenseEntity> findByAmountBetweenOrderByIdAsc(long minAmount, long maxAmount);
    Page<ExpenseEntity> findByAmountBetween(
            long minAmount, long maxAmount, Pageable pageable);
    Page<ExpenseEntity> findByCategoryAndAmountBetween(
            String category, long minAmount, long maxAmount, Pageable pageable);
    @Query(value = """
        SELECT category,
               COUNT_BIG(*) AS expenseCount,
               SUM(CAST(amount AS DECIMAL(38, 0))) AS totalAmount
        FROM dbo.expenses
            WHERE (
                    CAST(:monthStart AS DATE) IS NULL
                    OR expense_date BETWEEN CAST(:monthStart AS DATE) AND CAST(:monthEnd AS DATE)
                )
            GROUP BY category
            HAVING CAST(:minTotal AS DECIMAL(38, 0)) IS NULL
                OR SUM(CAST(amount AS DECIMAL(38, 0))) >= CAST(:minTotal AS DECIMAL(38, 0))
            ORDER BY totalAmount DESC, category ASC
        """, nativeQuery = true)
    List<ExpenseCategorySummary> summarizeByCategory(
            @Param("minTotal") Long minTotal,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd
    );

}
