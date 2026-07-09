INSERT INTO accounts (id, name, user_id, currency, type, system, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'Cash Wallet', '', NULL, 'PHP', 'CASH', TRUE, TRUE, NOW(), NOW());

INSERT INTO categories (id, name, icon, user_id, transaction_type, system, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'Freelance / Side Hustle', 'work', NULL, 'INCOME', TRUE, TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'Investments', 'trending_up', NULL, 'INCOME', TRUE, TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'Miscellaneous', 'add_circle_outline', NULL, 'INCOME', TRUE, TRUE, NOW(), NOW()),

       (gen_random_uuid(), 'Miscellaneous', 'add_circle_outline', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'Transportation', 'directions_car', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW())
       (gen_random_uuid(), 'Food & Dining', 'restaurant', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW());
