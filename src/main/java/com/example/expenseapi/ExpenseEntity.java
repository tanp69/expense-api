package com.example.expenseapi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDate;

@Entity
@Table(name = "expenses", schema = "dbo")
public class ExpenseEntity {

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    protected ExpenseEntity() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long amount;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String category;

    public ExpenseEntity(long amount, String category) {
        this(amount, category, null);
    }

    public void changeExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public ExpenseEntity(long amount, String category, LocalDate expenseDate) {
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
    }

    public void update(long amount, String category) {
        this.amount = amount;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }
}