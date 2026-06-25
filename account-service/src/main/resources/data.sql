-- Seed data for B2B accounts
-- Satisfies requirements for 3 sample B2B accounts with different tiers

-- Password for all accounts: "password" (BCrypt hashed)
-- BCrypt hash: $2a$10$gg9JZzUFZKIR7CP/CYbvWOIhCsrQ49oR98Effdrv.j6mED1USd8Cm

INSERT INTO accounts (id, email, password_hash, company_name, tier, credit_limit, current_balance, api_key, created_at, updated_at)
VALUES
    -- Account 1: STANDARD tier - ACME Auto Parts
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'admin@acme.com', '$2a$10$gg9JZzUFZKIR7CP/CYbvWOIhCsrQ49oR98Effdrv.j6mED1USd8Cm', 'ACME Auto Parts', 'STANDARD', 10000.00, 0.00, 'ak_acme_1234567890', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Account 2: SILVER tier - Globex Corporation
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'buyer@globex.com', '$2a$10$gg9JZzUFZKIR7CP/CYbvWOIhCsrQ49oR98Effdrv.j6mED1USd8Cm', 'Globex Corporation', 'SILVER', 25000.00, 0.00, 'ak_globex_2345678901', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Account 3: GOLD tier - Initech Industries
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'manager@initech.com', '$2a$10$gg9JZzUFZKIR7CP/CYbvWOIhCsrQ49oR98Effdrv.j6mED1USd8Cm', 'Initech Industries', 'GOLD', 50000.00, 0.00, 'ak_initech_3456789012', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;