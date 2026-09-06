--- Recurring Transactions table:
CREATE TABLE IF NOT EXISTS recurring_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    updated_by UUID NOT NULL,
    description VARCHAR(255) NULL,
    amount NUMERIC(12, 2) NOT NULL,
    variable_amount BOOLEAN NOT NULL DEFAULT FALSE,
    category_id UUID NULL,
    account_id UUID NOT NULL,
    day_of_month INTEGER NOT NULL,
    next_occurrence_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_recurring_transactions_category_id FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_recurring_transactions_account_id FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT chk_day_of_month CHECK (day_of_month >= 1 AND day_of_month <= 31)
);

CREATE INDEX IF NOT EXISTS idx_recurring_transactions_account_id ON recurring_transactions (account_id);
CREATE INDEX IF NOT EXISTS idx_recurring_transactions_category_id ON recurring_transactions (category_id);

GRANT INSERT, SELECT, UPDATE, DELETE ON recurring_transactions TO "financial_tracker_apps";
