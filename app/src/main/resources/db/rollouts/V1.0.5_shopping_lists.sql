--- Shopping Lists table:
CREATE TABLE IF NOT EXISTS shopping_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(255) NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(32) NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED')),
    user_id UUID NULL,
    category_id UUID NULL,
    budget NUMERIC(12, 2) NULL,
    final_amount NUMERIC(12, 2) NULL,
    completed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_shopping_lists_category_id FOREIGN KEY (category_id) REFERENCES categories (id)
);

--- Shopping Lists Shared Users junction table:
CREATE TABLE IF NOT EXISTS shopping_lists_shared_users (
    shopping_list_id UUID NOT NULL,
    shared_user_id UUID NOT NULL,
    PRIMARY KEY (shopping_list_id, shared_user_id),
    CONSTRAINT fk_shopping_lists_shared_users_list FOREIGN KEY (shopping_list_id) REFERENCES shopping_lists (id) ON DELETE CASCADE
);

--- Shopping Items table:
CREATE TABLE IF NOT EXISTS shopping_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(255) NULL,
    category VARCHAR(255) NULL,
    quantity NUMERIC(10, 4) NOT NULL DEFAULT 1,
    unit VARCHAR(64) NULL,
    price NUMERIC(12, 2) NULL,
    purchased BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(32) NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    notes VARCHAR(500) NULL,
    added_by UUID NULL,
    shopping_list_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_shopping_items_list FOREIGN KEY (shopping_list_id) REFERENCES shopping_lists (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shopping_items_list_id ON shopping_items (shopping_list_id);

GRANT INSERT, SELECT, UPDATE, DELETE ON shopping_lists, shopping_lists_shared_users, shopping_items TO "financial_tracker_apps";
