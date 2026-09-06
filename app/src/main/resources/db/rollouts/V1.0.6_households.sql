--- Households table:
CREATE TABLE IF NOT EXISTS households (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(255) NULL,
    leader_id UUID NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL
);

--- Household Members table:
CREATE TABLE IF NOT EXISTS household_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    user_id UUID NULL,
    access_level VARCHAR(32) NULL,
    status VARCHAR(32) NULL CHECK (status IN ('PENDING', 'ACTIVE', 'LEFT', 'REMOVED')),
    joined_at TIMESTAMPTZ NULL,
    household_id UUID NOT NULL,
    CONSTRAINT fk_household_members_household FOREIGN KEY (household_id) REFERENCES households (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_household_members_household_id ON household_members (household_id);
CREATE INDEX IF NOT EXISTS idx_household_members_user_id ON household_members (user_id);

--- Invitations table:
CREATE TABLE IF NOT EXISTS invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    inviter_user_id UUID NULL,
    invitee_user_id UUID NULL,
    proposed_role VARCHAR(32) NULL,
    status VARCHAR(32) NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    created_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    household_id UUID NOT NULL,
    CONSTRAINT fk_invitations_household FOREIGN KEY (household_id) REFERENCES households (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_invitations_household_id ON invitations (household_id);
CREATE INDEX IF NOT EXISTS idx_invitations_invitee_user_id ON invitations (invitee_user_id);

GRANT INSERT, SELECT, UPDATE, DELETE ON households, household_members, invitations TO "financial_tracker_apps";
