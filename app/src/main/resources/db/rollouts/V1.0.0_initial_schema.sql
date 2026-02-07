
--- Categories table:
-- Enable UUID generation (pgcrypto)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(128) NOT NULL,
    user_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_accounts_name_user_id UNIQUE (name, user_id)
);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts (user_id);
CREATE INDEX IF NOT EXISTS idxs_accounts_name ON accounts (name);

CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(128),
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_name_user_id UNIQUE (name, user_id)
);

CREATE INDEX IF NOT EXISTS idx_categories_name ON categories (name);
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories (user_id);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    account_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(10) NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    description TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_transactions_account_id FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transactions_category_id FOREIGN KEY (category_id) REFERENCES categories (id)
);
-- add a composite index for transaction date and transaction_type
CREATE INDEX IF NOT EXISTS idx_transactions_date_type ON transactions (transaction_date, transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions (account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions (category_id);
