USE [master];
GO

IF DB_ID(N'expense_setup_db') IS NULL
BEGIN
    CREATE DATABASE [expense_setup_db];
END;
GO

USE [expense_setup_db];
GO

IF OBJECT_ID(N'dbo.expenses', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.expenses (
        id BIGINT IDENTITY(1, 1) NOT NULL,
        amount BIGINT NOT NULL,
        category NVARCHAR(MAX) NOT NULL,
        expense_date DATE NULL,

        CONSTRAINT PK_expenses PRIMARY KEY (id),
        CONSTRAINT CK_expenses_amount_positive
            CHECK (amount > 0),
        CONSTRAINT CK_expenses_category_required
            CHECK (LEN(LTRIM(RTRIM(category))) > 0)
    );
END;
GO

IF DATABASE_PRINCIPAL_ID(N'expense_app') IS NULL
BEGIN
    CREATE USER [expense_app] FOR LOGIN [expense_app];
END;
GO

GRANT SELECT, INSERT, UPDATE, DELETE
ON OBJECT::dbo.expenses
TO [expense_app];
GO