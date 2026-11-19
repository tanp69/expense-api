package com.example.expenseapi;

import java.util.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.util.ReflectionTestUtils;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class ExpenseControllerTest {

    private ExpenseRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(ExpenseRepository.class);
        ExpenseService service = new ExpenseService(repository);

        mvc = MockMvcBuilders
                .standaloneSetup(new ExpenseController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void rejectsZeroAmount() throws Exception {
        mvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":0,"category":"food"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Amount must be greater than zero"));

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsBlankCategory() throws Exception {
        mvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":25000,"category":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Category is required"));

        verifyNoInteractions(repository);
    }

    @Test
    void returnsNotFoundWhenExpenseDoesNotExist() throws Exception {
        long missingId = 999999L;
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        mvc.perform(get("/expenses/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Expense not found"));

        verify(repository).findById(missingId);
    }

    @Test
    void returnsBadRequestWhenExpenseIdIsNotANumber() throws Exception {
        mvc.perform(get("/expenses/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(repository);
    }

    @Test
    void returnsExpenseWhenIdExists() throws Exception {
        long expenseId = 7L;

        ExpenseEntity expense = mock(ExpenseEntity.class);
        when(expense.getId()).thenReturn(expenseId);
        when(expense.getAmount()).thenReturn(42000L);
        when(expense.getCategory()).thenReturn("travel");

        when(repository.findById(expenseId)).thenReturn(Optional.of(expense));
        mvc.perform(get("/expenses/{id}", expenseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.amount").value(42000))
                .andExpect(jsonPath("$.category").value("travel"));

        verify(repository).findById(expenseId);
    }
    @Test
    void createsExpenseAndReturns201() throws Exception {
        ExpenseEntity savedExpense = mock(ExpenseEntity.class);
        when(savedExpense.getId()).thenReturn(7L);
        when(savedExpense.getAmount()).thenReturn(37000L);
        when(savedExpense.getCategory()).thenReturn("food");
        when(repository.save(any(ExpenseEntity.class))).thenReturn(savedExpense);

        mvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":37000,"category":"food"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.amount").value(37000))
                .andExpect(jsonPath("$.category").value("food"));

        ArgumentCaptor<ExpenseEntity> captor =
                ArgumentCaptor.forClass(ExpenseEntity.class);
        verify(repository).save(captor.capture());

        ExpenseEntity expenseToSave = captor.getValue();
        assertEquals(37000L, expenseToSave.getAmount());
        assertEquals("food", expenseToSave.getCategory());
    }
    @Test
    void updatesExistingExpenseAndReturns200() throws Exception {
        long expenseId = 7L;
        ExpenseEntity expense = new ExpenseEntity(25000L, "food");
        ReflectionTestUtils.setField(expense, "id", expenseId);
        when(repository.findById(expenseId)).thenReturn(Optional.of(expense));

        mvc.perform(put("/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"amount":42000,"category":"travel"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.amount").value(42000))
                .andExpect(jsonPath("$.category").value("travel"));

        verify(repository).findById(expenseId);
        assertEquals(42000L, expense.getAmount());
        assertEquals("travel", expense.getCategory());
    }

    @Test
    void rejectsUpdateWhenAmountIsZero() throws Exception {
        mvc.perform(put("/expenses/{id}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"amount":0,"category":"travel"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Amount must be greater than zero"));

        verifyNoInteractions(repository);
    }

    @Test
    void deletesExistingExpenseAndReturns204() throws Exception {
        long expenseId = 7L;

        when(repository.deleteExpenseById(expenseId)).thenReturn(1);
        mvc.perform(delete("/expenses/{id}", expenseId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(repository).deleteExpenseById(expenseId);
    }

    @Test
    void returnsNotFoundWhenDeletingMissingExpense() throws Exception {
        long missingId = 999999L;
        when(repository.deleteExpenseById(missingId)).thenReturn(0);

        mvc.perform(delete("/expenses/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Expense not found"));

        verify(repository).deleteExpenseById(missingId);
    }

    @Test
    void filtersExpensesByAmountAndCategory() throws Exception {
        ExpenseEntity foodExpense = new ExpenseEntity(50000L, "food");
        ExpenseEntity travelExpense = new ExpenseEntity(42000L, "travel");

        when(repository.findByAmountBetweenOrderByIdAsc(20000L, 50000L))
                .thenReturn(List.of(foodExpense, travelExpense));

        mvc.perform(get("/expenses")
                        .param("minAmount", "20000")
                        .param("maxAmount", "50000")
                        .param("category", "food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(50000))
                .andExpect(jsonPath("$[0].category").value("food"));

        verify(repository).findByAmountBetweenOrderByIdAsc(20000L, 50000L);
    }

    @Test
    void rejectsInvertedAmountRange() throws Exception {
        mvc.perform(get("/expenses")
                .param("minAmount", "50000")
                .param("maxAmount", "20000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid amount range"));

        verifyNoInteractions(repository);
    }





}