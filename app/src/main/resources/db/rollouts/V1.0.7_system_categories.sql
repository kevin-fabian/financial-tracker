INSERT INTO categories (id, name, icon, user_id, transaction_type, active, "system", local_id, created_at, updated_at)
VALUES
    -- Income categories
    ('a1b2c3d1-0000-0000-0000-000000000001', 'Salary', 'attach_money', NULL, 'INCOME', TRUE, TRUE, 'a1b2c3d1-0000-0000-0000-000000000001', NOW(), NOW()),
    ('a1b2c3d1-0000-0000-0000-000000000002', 'Business', 'storefront', NULL, 'INCOME', TRUE, TRUE, 'a1b2c3d1-0000-0000-0000-000000000002', NOW(), NOW()),
    ('a1b2c3d1-0000-0000-0000-000000000003', 'Side Hustle', 'trending_up', NULL, 'INCOME', TRUE, TRUE, 'a1b2c3d1-0000-0000-0000-000000000003', NOW(), NOW()),
    -- Expense categories
    ('a1b2c3d2-0000-0000-0000-000000000001', 'Utilities & Bills', 'bolt', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000001', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000002', 'Transportation', 'directions_car', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000002', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000003', 'Food & Groceries', 'restaurant', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000003', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000004', 'Debt & Loans', 'credit_card', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000004', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000005', 'Family & Kids', 'family_restroom', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000005', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000006', 'Housing & Rent', 'home', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000006', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000007', 'Shopping & Personal Care', 'shopping_bag', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000007', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000008', 'Education', 'school', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000008', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000009', 'Healthcare', 'medical_services', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000009', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000010', 'Hobbies & Lifestyle', 'sports_esports', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000010', NOW(), NOW()),
    ('a1b2c3d2-0000-0000-0000-000000000011', 'Others', 'category', NULL, 'EXPENSE', TRUE, TRUE, 'a1b2c3d2-0000-0000-0000-000000000011', NOW(), NOW());
