CREATE TABLE IF NOT EXISTS sync_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    user_id UUID NOT NULL,
    device_id UUID NOT NULL,
    user_device VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    direction VARCHAR(32) NOT NULL CHECK (direction IN ('PULL', 'PUSH')),
    last_synced_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sync_devices_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_sync_devices_user_id ON sync_devices (user_id);

GRANT INSERT, SELECT, UPDATE, DELETE ON sync_devices TO "financial_tracker_apps";
