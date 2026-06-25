-- Seed data for order-service
-- 5 sample orders across different accounts

-- Order 1: admin@acme.com - 3 items, PENDING status
INSERT INTO orders (id, account_id, status, total_amount, credit_used, created_at, updated_at)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'PENDING', 129.97, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_sku, product_name, quantity, unit_price, total_price)
VALUES
('b1b2c3d4-e5f6-7890-abcd-ef1234567890', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'BRK-001', 'Brake Pad Set - Front', 2, 29.99, 59.98),
('b1b2c3d4-e5f6-7890-abcd-ef1234567891', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'ENG-001', 'Oil Filter', 1, 149.99, 149.99),
('b1b2c3d4-e5f6-7890-abcd-ef1234567892', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'FLT-001', 'Engine Oil Filter', 1, 12.99, 12.99)
ON CONFLICT (id) DO NOTHING;

-- Order 2: buyer@globex.com - 2 items, SHIPPED status
INSERT INTO orders (id, account_id, status, total_amount, credit_used, created_at, updated_at)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567891', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'SHIPPED', 209.98, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_sku, product_name, quantity, unit_price, total_price)
VALUES
('b1b2c3d4-e5f6-7890-abcd-ef1234567893', 'a1b2c3d4-e5f6-7890-abcd-ef1234567891', 'ELE-001', 'Spark Plug Set', 2, 19.99, 39.98),
('b1b2c3d4-e5f6-7890-abcd-ef1234567894', 'a1b2c3d4-e5f6-7890-abcd-ef1234567891', 'SUS-001', 'Shock Absorber - Front', 1, 79.99, 79.99)
ON CONFLICT (id) DO NOTHING;

-- Order 3: manager@initech.com - 5 items, DELIVERED status
INSERT INTO orders (id, account_id, status, total_amount, credit_used, created_at, updated_at)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567892', 'c3d4e5f6-a7b8-9012-cdef-123456789012', 'DELIVERED', 459.95, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_sku, product_name, quantity, unit_price, total_price)
VALUES
('b1b2c3d4-e5f6-7890-abcd-ef1234567895', 'a1b2c3d4-e5f6-7890-abcd-ef1234567892', 'BRK-002', 'Brake Pad Set - Rear', 1, 49.99, 49.99),
('b1b2c3d4-e5f6-7890-abcd-ef1234567896', 'a1b2c3d4-e5f6-7890-abcd-ef1234567892', 'ENG-002', 'Air Filter', 2, 89.99, 179.98),
('b1b2c3d4-e5f6-7890-abcd-ef1234567897', 'a1b2c3d4-e5f6-7890-abcd-ef1234567892', 'ELE-002', 'Battery Terminal', 1, 39.99, 39.99),
('b1b2c3d4-e5f6-7890-abcd-ef1234567898', 'a1b2c3d4-e5f6-7890-abcd-ef1234567892', 'FLT-002', 'Cabin Air Filter', 2, 18.99, 37.98),
('b1b2c3d4-e5f6-7890-abcd-ef1234567899', 'a1b2c3d4-e5f6-7890-abcd-ef1234567892', 'SUS-002', 'Shock Absorber - Rear', 1, 119.99, 119.99)
ON CONFLICT (id) DO NOTHING;

-- Order 4: admin@acme.com - 1 item, CONFIRMED status
INSERT INTO orders (id, account_id, status, total_amount, credit_used, created_at, updated_at)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567893', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'CONFIRMED', 199.99, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_sku, product_name, quantity, unit_price, total_price)
VALUES
('b1b2c3d4-e5f6-7890-abcd-ef123456789a', 'a1b2c3d4-e5f6-7890-abcd-ef1234567893', 'ENG-003', 'Fuel Filter', 1, 199.99, 199.99)
ON CONFLICT (id) DO NOTHING;

-- Order 5: buyer@globex.com - 4 items, PENDING status
INSERT INTO orders (id, account_id, status, total_amount, credit_used, created_at, updated_at)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567894', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'PENDING', 319.96, 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_sku, product_name, quantity, unit_price, total_price)
VALUES
('b1b2c3d4-e5f6-7890-abcd-ef123456789b', 'a1b2c3d4-e5f6-7890-abcd-ef1234567894', 'BRK-003', 'Brake Rotor - Front', 2, 69.99, 139.98),
('b1b2c3d4-e5f6-7890-abcd-ef123456789c', 'a1b2c3d4-e5f6-7890-abcd-ef1234567894', 'ELE-003', 'Ignition Coil', 1, 59.99, 59.99),
('b1b2c3d4-e5f6-7890-abcd-ef123456789d', 'a1b2c3d4-e5f6-7890-abcd-ef1234567894', 'FLT-003', 'Transmission Filter', 1, 24.99, 24.99),
('b1b2c3d4-e5f6-7890-abcd-ef123456789e', 'a1b2c3d4-e5f6-7890-abcd-ef1234567894', 'SUS-003', 'Strut Assembly', 1, 159.99, 159.99)
ON CONFLICT (id) DO NOTHING;
