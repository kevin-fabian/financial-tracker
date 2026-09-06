--- Budgets table:
CREATE TABLE IF NOT EXISTS budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    user_id UUID NOT NULL,
    updated_by UUID NULL,
    period VARCHAR(32) NOT NULL CHECK (period IN ('MONTHLY', 'WEEKLY', 'YEARLY', 'CUSTOM')),
    category_id UUID NULL,
    allocated NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_budgets_category_id FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX IF NOT EXISTS idx_budgets_user_id ON budgets (user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_category_id ON budgets (category_id);

GRANT INSERT, SELECT, UPDATE, DELETE ON budgets TO "financial_tracker_apps";
