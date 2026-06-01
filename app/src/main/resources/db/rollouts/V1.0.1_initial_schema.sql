
--- Categories table:
-- Enable UUID generation (pgcrypto)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Icons table
CREATE TABLE IF NOT EXISTS icons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    code_point INTEGER NOT NULL,
    font_family VARCHAR(128) NOT NULL,
    icon_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_icons_code_point_font_family ON icons (code_point, font_family);

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(128) NOT NULL,
    user_id UUID NOT NULL,
    icon_id UUID NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_accounts_name_user_id UNIQUE (name, user_id),
    CONSTRAINT chk_accounts_type CHECK (type IN ('CASH', 'BANK_ACCOUNT', 'CREDIT_CARD', 'E_WALLET', 'INVESTMENT', 'LOAN', 'OTHER')),
    CONSTRAINT fk_accounts_icon_id FOREIGN KEY (icon_id) REFERENCES icons (id)
);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts (user_id);
CREATE INDEX IF NOT EXISTS idxs_accounts_name ON accounts (name);

CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(128),
    user_id UUID NOT NULL,
    icon_id UUID NULL,
    transaction_type VARCHAR(10) NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_name_user_id UNIQUE (name, user_id, transaction_type),
    CONSTRAINT fk_categories_icon_id FOREIGN KEY (icon_id) REFERENCES icons (id)
);

CREATE INDEX IF NOT EXISTS idx_categories_name_transaction_type_user_id ON categories (name, transaction_type, user_id);
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
