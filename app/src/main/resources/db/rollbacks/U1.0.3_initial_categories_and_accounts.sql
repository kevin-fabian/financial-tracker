DELETE FROM accounts
WHERE system = TRUE
  AND user_id IS NULL;

DELETE FROM categories
WHERE system = TRUE
  AND user_id IS NULL;
