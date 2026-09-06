REVOKE INSERT, SELECT, UPDATE, DELETE ON households, household_members, invitations FROM "financial_tracker_apps";
DROP TABLE IF EXISTS invitations CASCADE;
DROP TABLE IF EXISTS household_members CASCADE;
DROP TABLE IF EXISTS households CASCADE;
