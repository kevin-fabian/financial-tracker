CREATE TABLE IF NOT EXISTS sync_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    user_id UUID NOT NULL,
    user_device VARCHAR(128) NULL,
    resource_type VARCHAR(32) NOT NULL CHECK (resource_type IN ('ACCOUNTS', 'CATEGORIES', 'TRANSACTIONS')),
    sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (sync_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sync_states_user_device_resource_type UNIQUE (user_id, user_device, resource_type)
);

CREATE INDEX IF NOT EXISTS idx_sync_states_user_id ON sync_states (user_id);

GRANT INSERT, SELECT, UPDATE, DELETE ON sync_states TO "financial_tracker_apps";
