REVOKE INSERT, SELECT, UPDATE, DELETE ON shopping_lists, shopping_lists_shared_users, shopping_items FROM "financial_tracker_apps";
DROP TABLE IF EXISTS shopping_items CASCADE;
DROP TABLE IF EXISTS shopping_lists_shared_users CASCADE;
DROP TABLE IF EXISTS shopping_lists CASCADE;
