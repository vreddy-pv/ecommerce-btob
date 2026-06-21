-- Seed data for B2B Auto Parts Catalog
-- 5 categories and 20 products with tier pricing

-- Categories
INSERT INTO categories (id, name, parent_id, sort_order) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Brakes', NULL, 1),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Engine', NULL, 2),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'Electrical', NULL, 3),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'Suspension', NULL, 4),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'Filters', NULL, 5);

-- Products (20 auto parts)
INSERT INTO products (id, sku, name, description, base_price, inventory_level, category_id, is_active, created_at, updated_at) VALUES
-- Brakes (5 products)
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'BRK-001', 'Front Brake Pads', 'Ceramic front brake pads for passenger vehicles', 45.99, 150, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b02', 'BRK-002', 'Rear Brake Pads', 'Ceramic rear brake pads for passenger vehicles', 39.99, 120, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b03', 'BRK-003', 'Brake Rotors', 'Front brake rotors, vented', 65.50, 80, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b04', 'BRK-004', 'Brake Caliper', 'Front brake caliper assembly', 125.00, 45, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b05', 'BRK-005', 'Brake Fluid', 'DOT 4 brake fluid, 32oz', 12.99, 200, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', true, NOW(), NOW()),

-- Engine (5 products)
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b06', 'ENG-001', 'Spark Plugs', 'Iridium spark plugs, set of 4', 24.99, 300, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b07', 'ENG-002', 'Engine Oil', 'Synthetic 5W-30 engine oil, 5qt', 29.99, 250, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b08', 'ENG-003', 'Oil Filter', 'Premium oil filter', 8.99, 500, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b09', 'ENG-004', 'Air Filter', 'Engine air filter', 15.99, 400, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b10', 'ENG-005', 'Timing Belt', 'Timing belt kit with tensioner', 89.99, 60, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', true, NOW(), NOW()),

-- Electrical (5 products)
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11', 'ELC-001', 'Car Battery', '12V car battery, 600 CCA', 129.99, 75, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b12', 'ELC-002', 'Alternator', 'Remanufactured alternator', 149.99, 40, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b13', 'ELC-003', 'Starter Motor', 'Remanufactured starter motor', 99.99, 55, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b14', 'ELC-004', 'Headlights', 'LED headlight bulbs, pair', 34.99, 180, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b15', 'ELC-005', 'Fuses', 'Assorted automotive fuses, 50-pack', 9.99, 600, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', true, NOW(), NOW()),

-- Suspension (3 products)
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b16', 'SUS-001', 'Shock Absorbers', 'Front shock absorbers, pair', 79.99, 90, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b17', 'SUS-002', 'Struts', 'Front strut assembly', 119.99, 65, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b18', 'SUS-003', 'Control Arms', 'Lower control arm with ball joint', 89.99, 50, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', true, NOW(), NOW()),

-- Filters (2 products)
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b19', 'FLT-001', 'Cabin Air Filter', 'Cabin air filter with activated carbon', 19.99, 350, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', true, NOW(), NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b20', 'FLT-002', 'Fuel Filter', 'Inline fuel filter', 14.99, 280, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', true, NOW(), NOW());

-- Tier Pricing (SILVER 10%, GOLD 15%, PLATINUM 20% discount)
INSERT INTO tier_pricing (id, product_id, tier, price) VALUES
-- BRK-001 Front Brake Pads
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'SILVER', 41.39),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'GOLD', 39.09),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'PLATINUM', 36.79),

-- BRK-002 Rear Brake Pads
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c04', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b02', 'SILVER', 35.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b02', 'GOLD', 33.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c06', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b02', 'PLATINUM', 31.99),

-- BRK-003 Brake Rotors
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c07', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b03', 'SILVER', 58.95),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c08', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b03', 'GOLD', 55.68),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c09', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b03', 'PLATINUM', 52.40),

-- ENG-001 Spark Plugs
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c10', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b06', 'SILVER', 22.49),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c11', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b06', 'GOLD', 21.24),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c12', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b06', 'PLATINUM', 19.99),

-- ENG-002 Engine Oil
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c13', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b07', 'SILVER', 26.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c14', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b07', 'GOLD', 25.49),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c15', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b07', 'PLATINUM', 23.99),

-- ELC-001 Car Battery
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c16', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11', 'SILVER', 116.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c17', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11', 'GOLD', 110.49),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c18', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b11', 'PLATINUM', 103.99),

-- ELC-002 Alternator
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c19', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b12', 'SILVER', 134.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c20', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b12', 'GOLD', 127.49),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c21', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b12', 'PLATINUM', 119.99),

-- SUS-001 Shock Absorbers
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c22', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b16', 'SILVER', 71.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c23', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b16', 'GOLD', 67.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c24', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b16', 'PLATINUM', 63.99),

-- FLT-001 Cabin Air Filter
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c25', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b19', 'SILVER', 17.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c26', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b19', 'GOLD', 16.99),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c27', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b19', 'PLATINUM', 15.99);