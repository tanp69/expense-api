package com.example.expenseapi;

import java.time.LocalDate;
import java.util.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.hamcrest.Matchers.contains;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:sqlserver://127.0.0.1:1433;"
                + "databaseName=expense_test_db;encrypt=true;trustServerCertificate=true")
@AutoConfigureMockMvc
@Transactional
class ExpenseApiIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ExpenseRepository repository;

    @Test
    void createsExpenseAndWritesItToSqlServer() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        MvcResult result = mvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":37000,"category":"food"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        Integer matchingRows = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.expenses
                WHERE id = ? AND amount = ? AND category = ?
                """, Integer.class, id.longValue(), 37000L, "food");

        assertEquals(1, matchingRows);
    }

    @Test
    void newtest() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        MvcResult result = mvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":69000,"category":"chatgpt","expenseDate":"2026-11-18"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.expenseDate").value("2026-11-18"))
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        Integer matchingRows = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.expenses
                WHERE id = ? AND amount = ? AND category = ? AND expense_date = ?
                """, Integer.class, id.longValue(), 69000L, "chatgpt", java.sql.Date.valueOf("2026-11-18"));

        assertEquals(1, matchingRows);
    }

    @Test
    void rejectsZeroAmountWithoutInsertingRow() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        String countSql = "SELECT COUNT(*) FROM dbo.expenses";
        Integer countBefore = jdbc.queryForObject(countSql, Integer.class);

        mvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"amount":0,"category":"food"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Amount must be greater than zero"));

        Integer countAfter = jdbc.queryForObject(countSql, Integer.class);
        assertEquals(countBefore, countAfter);
    }

    @Test
    void updatesExistingExpenseInSqlServer() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        ExpenseEntity expense =
                repository.saveAndFlush(new ExpenseEntity(25000L, "food"));
        long id = expense.getId();

        mvc.perform(put("/expenses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"amount":42000,"category":"travel"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(42000))
                .andExpect(jsonPath("$.category").value("travel"));

        repository.flush();

        Integer matchingRows = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.expenses
            WHERE id = ? AND amount = ? AND category = ?
            """, Integer.class, id, 42000L, "travel");

        assertEquals(1, matchingRows);
    }

    @Test
    void rejectsInvalidUpdateAndKeepsOriginalValues() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        ExpenseEntity expense =
                repository.saveAndFlush(new ExpenseEntity(25000L, "food"));
        long id = expense.getId();

        mvc.perform(put("/expenses/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"amount":0,"category":"travel"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        repository.flush();

        Integer matchingRows = jdbc.queryForObject("""
        SELECT COUNT(*)
        FROM dbo.expenses
        WHERE id = ? AND amount = ? AND category = ?
        """, Integer.class, id, 25000L, "food");

        assertEquals(1, matchingRows);
    }
    @Test
    void deletesExpenseFromSqlServer() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        ExpenseEntity expense =
                repository.saveAndFlush(new ExpenseEntity(12345L, "food"));
        long id = expense.getId();

        mvc.perform(delete("/expenses/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Integer matchingRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.expenses WHERE id = ?",
                Integer.class,
                id
        );

        assertEquals(0, matchingRows);

        mvc.perform(delete("/expenses/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Expense not found"));

    }

    @Test
    void filtersAndSortsBeforePaginating() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        String category = "food-page-test";
        repository.saveAllAndFlush(List.of(
                new ExpenseEntity(20000L, category),
                new ExpenseEntity(50000L, category),
                new ExpenseEntity(60000L, category),
                new ExpenseEntity(45000L, "travel-page-test")
        ));

        mvc.perform(get("/expenses/page")
                        .param("page", "0")
                        .param("size", "1")
                        .param("category", category)
                        .param("minAmount", "20000")
                        .param("maxAmount", "50000")
                        .param("sortBy", "amount")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].amount").value(50000))
                .andExpect(jsonPath("$.items[0].category").value(category))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/expenses/page")
                        .param("page", "1")
                        .param("size", "1")
                        .param("category", category)
                        .param("minAmount", "20000")
                        .param("maxAmount", "50000")
                        .param("sortBy", "amount")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].amount").value(20000))
                .andExpect(jsonPath("$.items[0].category").value(category))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void summarizesExpensesByCategory() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        repository.saveAllAndFlush(List.of(
                new ExpenseEntity(20000L, "food-summary-test"),
                new ExpenseEntity(35000L, "food-summary-test"),
                new ExpenseEntity(60000L, "travel-summary-test")
        ));

        mvc.perform(get("/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.category == 'food-summary-test')].expenseCount"
                ).value(2))
                .andExpect(jsonPath("$[?(@.category == 'food-summary-test')].totalAmount").value(55000))
                .andExpect(jsonPath("$[?(@.category == 'travel-summary-test')].expenseCount").value(1))
                .andExpect(jsonPath("$[?(@.category == 'travel-summary-test')].totalAmount").value(60000))
                .andExpect(jsonPath(
                        "$[?(@.category == 'food-summary-test' || " +
                                "@.category == 'travel-summary-test')].category"
                ).value(contains("travel-summary-test", "food-summary-test")));

        mvc.perform(get("/expenses/summary").param("minTotal", "60000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category == 'travel-summary-test')].totalAmount").value(60000))
                .andExpect(jsonPath("$[?(@.category == 'food-summary-test')]").isEmpty());

        mvc.perform(get("/expenses/summary").param("minTotal", "55000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category == 'food-summary-test')].expenseCount").value(2));
    }

    @Test
    void practice() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        repository.saveAllAndFlush(List.of(
                new ExpenseEntity(40000L, "food-moth-test", LocalDate.of(2026,10,31)),
                new ExpenseEntity(10000L, "food-moth-test", LocalDate.of(2026, 11, 1)),
                new ExpenseEntity(20000L, "food-moth-test", LocalDate.of(2026, 11, 30)),
                new ExpenseEntity(50000L, "food-moth-test", LocalDate.of(2026, 12, 1)),
                new ExpenseEntity(60000L, "food-moth-test"),
                new ExpenseEntity(15000L, "travel-moth-test", LocalDate.of(2026,11,15))
        ));

        mvc.perform(get("/expenses/summary")
                        .param("month", "2026-11")
                        .param("minTotal", "30000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category == 'food-moth-test')].expenseCount").value(2))
                .andExpect(jsonPath("$[?(@.category == 'food-moth-test')].totalAmount").value(30000))
                .andExpect(jsonPath("$[?(@.category == 'travel-moth-test')].expenseCount").isEmpty());
    }

    @Test
    void exam() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        ExpenseEntity expense =
                repository.saveAndFlush(new ExpenseEntity(25000L, "food", LocalDate.of(2026,10,31)));
        long id = expense.getId();

        mvc.perform(patch("/expenses/{id}/date", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"expenseDate":"2026-11-01"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseDate").value("2026-11-01"));

        repository.flush();

        Integer matchingRows = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.expenses
            WHERE id = ? AND amount = ? AND category = ? AND expense_date = ?
            """, Integer.class, id, 25000L, "food", java.sql.Date.valueOf("2026-11-01"));

        assertEquals(1, matchingRows);
    }

    @Test
    void movesExpenseBetweenMonthlySummaries() throws Exception {
        assertEquals("expense_test_db",
                jdbc.queryForObject("SELECT DB_NAME()", String.class));

        ExpenseEntity movingExpense = repository.saveAndFlush(
                new ExpenseEntity(
                        25000L,
                        "move-month-test",
                        LocalDate.of(2026, 10, 31)
                )
        );

        repository.saveAndFlush(
                new ExpenseEntity(
                        10000L,
                        "move-month-test",
                        LocalDate.of(2026, 11, 15)
                )
        );

        // Kiểm tra tháng 10 trước khi sửa ngày.
        mvc.perform(get("/expenses/summary")
                        .param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.category == 'move-month-test')].expenseCount"
                ).value(1))
                .andExpect(jsonPath(
                        "$[?(@.category == 'move-month-test')].totalAmount"
                ).value(25000));

        // Chuyển khoản A sang tháng 11.
        mvc.perform(patch("/expenses/{id}/date", movingExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"expenseDate":"2026-11-01"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseDate").value("2026-11-01"));

        repository.flush();

        // Tự bổ sung các lượt GET kiểm tra sau khi sửa tại đây.
        mvc.perform(get("/expenses/summary").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category == 'move-month-test')]").isEmpty());

        mvc.perform(get("/expenses/summary").param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.category == 'move-month-test')].expenseCount").value(2))
                .andExpect(jsonPath("$[?(@.category == 'move-month-test')].totalAmount").value(35000));
    }


}