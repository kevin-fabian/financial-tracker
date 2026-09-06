INSERT INTO categories (id, name, icon, user_id, transaction_type, active, "system", created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Utilities & Bills', 'bolt', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Transportation', 'directions_car', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Food & Groceries', 'restaurant', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Debt & Loans', 'credit_card', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Family & Kids', 'family_restroom', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Housing & Rent', 'home', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Shopping & Personal Care', 'shopping_bag', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Education', 'school', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Healthcare', 'medical_services', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Hobbies & Lifestyle', 'sports_esports', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'Others', 'category', NULL, 'EXPENSE', TRUE, TRUE, NOW(), NOW());
