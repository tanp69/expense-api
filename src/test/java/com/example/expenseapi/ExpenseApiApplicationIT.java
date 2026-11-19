package com.example.expenseapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:sqlserver://127.0.0.1:1433;"
                + "databaseName=expense_test_db;"
                + "encrypt=true;trustServerCertificate=true")
class ExpenseApiApplicationIT {

    @Test
    void contextLoads() {
    }
}