CREATE TABLE IF NOT EXISTS sync_changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    user_id UUID NOT NULL,
    user_device VARCHAR(64) NOT NULL,
    sequence_id BIGINT NOT NULL,
    entity_type VARCHAR(32) NOT NULL CHECK (entity_type IN ('ACCOUNTS', 'CATEGORIES', 'TRANSACTIONS')),
    entity_id UUID NOT NULL,
    operation VARCHAR(32) NOT NULL CHECK (operation IN ('CREATE', 'UPDATE', 'DELETE')),
    payload JSON NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sync_changes_user_id ON sync_changes (user_id);
CREATE INDEX IF NOT EXISTS idx_sync_changes_user_id_device ON sync_changes (user_id, user_device);
CREATE INDEX IF NOT EXISTS idx_sync_changes_user_id_sequence ON sync_changes (user_id, sequence_id);

GRANT INSERT, SELECT, UPDATE, DELETE ON sync_changes TO "financial_tracker_apps";
