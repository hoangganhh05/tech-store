-- US-15.6 demo seed data for Đăng Tùng Mobile.
--
-- Prerequisite: start the Backend once so Flyway has applied V1...V27, or use
-- an existing database at the current application schema version.
--
-- This script is deliberately idempotent. It only creates or replaces records
-- marked [Demo], DEMO-*, or DEMO-2026-*; it does not truncate application data.
-- Run it only in a local, demo, or staging database. Do not use demo accounts
-- or their password in production.

SET NAMES utf8mb4;
START TRANSACTION;

SET @seed_now := UTC_TIMESTAMP();
SET @seed_note := 'US-15.6 demo seed';

-- Demo catalog hierarchy
INSERT INTO categories (name, description, parent_id, image_url, display_order, is_active, created_at, updated_at)
SELECT '[Demo] Sạc & Cáp', 'Nhóm phụ kiện sạc dùng để trình diễn.', NULL,
       'https://placehold.co/600x400/0f172a/ffffff?text=Demo+Sac+Cap', 900, TRUE, @seed_now, @seed_now
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '[Demo] Sạc & Cáp');

INSERT INTO categories (name, description, parent_id, image_url, display_order, is_active, created_at, updated_at)
SELECT '[Demo] Phụ kiện bảo vệ', 'Nhóm ốp lưng và phụ kiện bảo vệ dùng để trình diễn.', NULL,
       'https://placehold.co/600x400/1e293b/ffffff?text=Demo+Bao+Ve', 901, TRUE, @seed_now, @seed_now
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '[Demo] Phụ kiện bảo vệ');

INSERT INTO categories (name, description, parent_id, image_url, display_order, is_active, created_at, updated_at)
SELECT '[Demo] Âm thanh', 'Nhóm tai nghe dùng để trình diễn.', NULL,
       'https://placehold.co/600x400/334155/ffffff?text=Demo+Am+Thanh', 902, TRUE, @seed_now, @seed_now
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '[Demo] Âm thanh');

INSERT INTO categories (name, description, parent_id, image_url, display_order, is_active, created_at, updated_at)
SELECT '[Demo] Pin dự phòng', 'Nhóm pin dự phòng dùng để trình diễn.', NULL,
       'https://placehold.co/600x400/475569/ffffff?text=Demo+Pin', 903, TRUE, @seed_now, @seed_now
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '[Demo] Pin dự phòng');

INSERT INTO categories (name, description, parent_id, image_url, display_order, is_active, created_at, updated_at)
SELECT '[Demo] Củ sạc', 'Củ sạc demo thuộc nhóm Sạc & Cáp.', parent.id,
       'https://placehold.co/600x400/0f766e/ffffff?text=Demo+Cu+Sac', 910, TRUE, @seed_now, @seed_now
FROM categories parent
WHERE parent.name = '[Demo] Sạc & Cáp'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = '[Demo] Củ sạc');

INSERT INTO categories (name, description, parent_id, image_url, display_order, is_active, created_at, updated_at)
SELECT '[Demo] Cáp sạc', 'Cáp sạc demo thuộc nhóm Sạc & Cáp.', parent.id,
       'https://placehold.co/600x400/0369a1/ffffff?text=Demo+Cap+Sac', 911, TRUE, @seed_now, @seed_now
FROM categories parent
WHERE parent.name = '[Demo] Sạc & Cáp'
  AND NOT EXISTS (SELECT 1 FROM categories WHERE name = '[Demo] Cáp sạc');

UPDATE categories
SET is_active = TRUE, updated_at = @seed_now
WHERE name LIKE '[Demo]%';

-- Demo brands
INSERT INTO brands (name, logo_url, description, created_at, updated_at) VALUES
  ('[Demo] Anker', 'https://placehold.co/240x120/111827/ffffff?text=Anker', 'Thương hiệu sạc demo.', @seed_now, @seed_now),
  ('[Demo] Baseus', 'https://placehold.co/240x120/1d4ed8/ffffff?text=Baseus', 'Thương hiệu cáp demo.', @seed_now, @seed_now),
  ('[Demo] Ugreen', 'https://placehold.co/240x120/166534/ffffff?text=Ugreen', 'Thương hiệu pin demo.', @seed_now, @seed_now),
  ('[Demo] ESR', 'https://placehold.co/240x120/7c3aed/ffffff?text=ESR', 'Thương hiệu ốp lưng demo.', @seed_now, @seed_now),
  ('[Demo] JBL', 'https://placehold.co/240x120/b45309/ffffff?text=JBL', 'Thương hiệu tai nghe demo.', @seed_now, @seed_now)
ON DUPLICATE KEY UPDATE
  logo_url = VALUES(logo_url), description = VALUES(description), updated_at = VALUES(updated_at);

-- Demo products. The unique (name, brand) key makes these upserts safe to rerun.
INSERT INTO products (name, description, brand_id, category_id, status, created_at, updated_at, is_deleted, deleted_at)
SELECT '[Demo] Anker Nano Charger 20W', 'Củ sạc nhanh USB-C nhỏ gọn, dùng để demo biến thể màu và cảnh báo tồn kho.', b.id, c.id,
       'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM brands b CROSS JOIN categories c
WHERE b.name = '[Demo] Anker' AND c.name = '[Demo] Củ sạc'
ON DUPLICATE KEY UPDATE description = VALUES(description), category_id = VALUES(category_id), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO products (name, description, brand_id, category_id, status, created_at, updated_at, is_deleted, deleted_at)
SELECT '[Demo] Baseus USB-C 100W Cable', 'Cáp USB-C 100W dùng để demo sản phẩm hết hàng.', b.id, c.id,
       'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM brands b CROSS JOIN categories c
WHERE b.name = '[Demo] Baseus' AND c.name = '[Demo] Cáp sạc'
ON DUPLICATE KEY UPDATE description = VALUES(description), category_id = VALUES(category_id), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO products (name, description, brand_id, category_id, status, created_at, updated_at, is_deleted, deleted_at)
SELECT '[Demo] Ugreen 10000mAh Power Bank', 'Pin dự phòng 10000mAh dùng để demo checkout và đơn đang giao.', b.id, c.id,
       'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM brands b CROSS JOIN categories c
WHERE b.name = '[Demo] Ugreen' AND c.name = '[Demo] Pin dự phòng'
ON DUPLICATE KEY UPDATE description = VALUES(description), category_id = VALUES(category_id), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO products (name, description, brand_id, category_id, status, created_at, updated_at, is_deleted, deleted_at)
SELECT '[Demo] ESR Classic MagSafe Case', 'Ốp lưng MagSafe dùng để demo voucher và đơn hoàn thành.', b.id, c.id,
       'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM brands b CROSS JOIN categories c
WHERE b.name = '[Demo] ESR' AND c.name = '[Demo] Phụ kiện bảo vệ'
ON DUPLICATE KEY UPDATE description = VALUES(description), category_id = VALUES(category_id), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO products (name, description, brand_id, category_id, status, created_at, updated_at, is_deleted, deleted_at)
SELECT '[Demo] JBL Tune USB-C Earphones', 'Tai nghe USB-C dùng để demo đơn đã huỷ và hoàn tồn.', b.id, c.id,
       'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM brands b CROSS JOIN categories c
WHERE b.name = '[Demo] JBL' AND c.name = '[Demo] Âm thanh'
ON DUPLICATE KEY UPDATE description = VALUES(description), category_id = VALUES(category_id), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

-- Product variants cover normal, low-stock and out-of-stock UI states.
INSERT INTO product_variants (product_id, sku, color, storage, price, original_price, stock_quantity, status, created_at, updated_at, is_deleted, deleted_at)
SELECT id, 'DEMO-ANKER-20W-WHITE', 'Trắng', NULL, 299000, 349000, 25, 'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM products WHERE name = '[Demo] Anker Nano Charger 20W'
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id), color = VALUES(color), price = VALUES(price),
                        original_price = VALUES(original_price), stock_quantity = VALUES(stock_quantity), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO product_variants (product_id, sku, color, storage, price, original_price, stock_quantity, status, created_at, updated_at, is_deleted, deleted_at)
SELECT id, 'DEMO-ANKER-20W-BLACK', 'Đen', NULL, 299000, 349000, 3, 'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM products WHERE name = '[Demo] Anker Nano Charger 20W'
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id), color = VALUES(color), price = VALUES(price),
                        original_price = VALUES(original_price), stock_quantity = VALUES(stock_quantity), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO product_variants (product_id, sku, color, storage, price, original_price, stock_quantity, status, created_at, updated_at, is_deleted, deleted_at)
SELECT id, 'DEMO-BASEUS-C100-BLACK', 'Đen', '1 m', 129000, 159000, 0, 'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM products WHERE name = '[Demo] Baseus USB-C 100W Cable'
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id), color = VALUES(color), storage = VALUES(storage), price = VALUES(price),
                        original_price = VALUES(original_price), stock_quantity = VALUES(stock_quantity), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO product_variants (product_id, sku, color, storage, price, original_price, stock_quantity, status, created_at, updated_at, is_deleted, deleted_at)
SELECT id, 'DEMO-UGREEN-PB10K-GRAY', 'Xám', '10000 mAh', 499000, 559000, 12, 'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM products WHERE name = '[Demo] Ugreen 10000mAh Power Bank'
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id), color = VALUES(color), storage = VALUES(storage), price = VALUES(price),
                        original_price = VALUES(original_price), stock_quantity = VALUES(stock_quantity), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO product_variants (product_id, sku, color, storage, price, original_price, stock_quantity, status, created_at, updated_at, is_deleted, deleted_at)
SELECT id, 'DEMO-ESR-MAGSAFE-CLEAR', 'Trong suốt', 'iPhone 15', 249000, 299000, 20, 'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM products WHERE name = '[Demo] ESR Classic MagSafe Case'
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id), color = VALUES(color), storage = VALUES(storage), price = VALUES(price),
                        original_price = VALUES(original_price), stock_quantity = VALUES(stock_quantity), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

INSERT INTO product_variants (product_id, sku, color, storage, price, original_price, stock_quantity, status, created_at, updated_at, is_deleted, deleted_at)
SELECT id, 'DEMO-JBL-USBC-WHITE', 'Trắng', NULL, 349000, 399000, 8, 'ACTIVE', @seed_now, @seed_now, FALSE, NULL
FROM products WHERE name = '[Demo] JBL Tune USB-C Earphones'
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id), color = VALUES(color), price = VALUES(price),
                        original_price = VALUES(original_price), stock_quantity = VALUES(stock_quantity), status = 'ACTIVE',
                        updated_at = VALUES(updated_at), is_deleted = FALSE, deleted_at = NULL;

-- Rebuild only the images/specifications belonging to [Demo] products.
DELETE pi FROM product_images pi
JOIN products p ON p.id = pi.product_id
WHERE p.name LIKE '[Demo]%';

INSERT INTO product_images (product_id, variant_id, image_url, is_primary, display_order, created_at, updated_at)
SELECT p.id, NULL, 'https://placehold.co/600x600/111827/ffffff?text=Demo+Anker+20W', TRUE, 0, @seed_now, @seed_now
FROM products p WHERE p.name = '[Demo] Anker Nano Charger 20W'
UNION ALL
SELECT p.id, v.id, 'https://placehold.co/600x600/334155/ffffff?text=Demo+Anker+Black', FALSE, 1, @seed_now, @seed_now
FROM products p JOIN product_variants v ON v.product_id = p.id AND v.sku = 'DEMO-ANKER-20W-BLACK'
WHERE p.name = '[Demo] Anker Nano Charger 20W'
UNION ALL
SELECT p.id, NULL, 'https://placehold.co/600x600/1d4ed8/ffffff?text=Demo+Baseus+100W', TRUE, 0, @seed_now, @seed_now
FROM products p WHERE p.name = '[Demo] Baseus USB-C 100W Cable'
UNION ALL
SELECT p.id, NULL, 'https://placehold.co/600x600/166534/ffffff?text=Demo+Ugreen+10000mAh', TRUE, 0, @seed_now, @seed_now
FROM products p WHERE p.name = '[Demo] Ugreen 10000mAh Power Bank'
UNION ALL
SELECT p.id, NULL, 'https://placehold.co/600x600/7c3aed/ffffff?text=Demo+ESR+MagSafe', TRUE, 0, @seed_now, @seed_now
FROM products p WHERE p.name = '[Demo] ESR Classic MagSafe Case'
UNION ALL
SELECT p.id, NULL, 'https://placehold.co/600x600/b45309/ffffff?text=Demo+JBL+USB-C', TRUE, 0, @seed_now, @seed_now
FROM products p WHERE p.name = '[Demo] JBL Tune USB-C Earphones';

DELETE ps FROM product_specifications ps
JOIN products p ON p.id = ps.product_id
WHERE p.name LIKE '[Demo]%';

INSERT INTO product_specifications (product_id, spec_key, spec_value, display_order, created_at, updated_at)
SELECT id, 'Công suất', '20W USB-C Power Delivery', 1, @seed_now, @seed_now FROM products WHERE name = '[Demo] Anker Nano Charger 20W'
UNION ALL SELECT id, 'Màu sắc', 'Trắng, Đen', 2, @seed_now, @seed_now FROM products WHERE name = '[Demo] Anker Nano Charger 20W'
UNION ALL SELECT id, 'Công suất', '100W', 1, @seed_now, @seed_now FROM products WHERE name = '[Demo] Baseus USB-C 100W Cable'
UNION ALL SELECT id, 'Dung lượng', '10000 mAh', 1, @seed_now, @seed_now FROM products WHERE name = '[Demo] Ugreen 10000mAh Power Bank'
UNION ALL SELECT id, 'Tương thích', 'iPhone 15', 1, @seed_now, @seed_now FROM products WHERE name = '[Demo] ESR Classic MagSafe Case'
UNION ALL SELECT id, 'Kết nối', 'USB-C', 1, @seed_now, @seed_now FROM products WHERE name = '[Demo] JBL Tune USB-C Earphones';

-- Inventory and an auditable import transaction for each demo variant.
INSERT INTO inventories (variant_id, quantity_on_hand, quantity_reserved, low_stock_threshold, version, updated_at)
SELECT id, 25, 0, 5, 0, @seed_now FROM product_variants WHERE sku = 'DEMO-ANKER-20W-WHITE'
ON DUPLICATE KEY UPDATE quantity_on_hand = VALUES(quantity_on_hand), quantity_reserved = 0, low_stock_threshold = 5, version = VALUES(version), updated_at = VALUES(updated_at);
INSERT INTO inventories (variant_id, quantity_on_hand, quantity_reserved, low_stock_threshold, version, updated_at)
SELECT id, 3, 0, 5, 0, @seed_now FROM product_variants WHERE sku = 'DEMO-ANKER-20W-BLACK'
ON DUPLICATE KEY UPDATE quantity_on_hand = VALUES(quantity_on_hand), quantity_reserved = 0, low_stock_threshold = 5, version = VALUES(version), updated_at = VALUES(updated_at);
INSERT INTO inventories (variant_id, quantity_on_hand, quantity_reserved, low_stock_threshold, version, updated_at)
SELECT id, 0, 0, 5, 0, @seed_now FROM product_variants WHERE sku = 'DEMO-BASEUS-C100-BLACK'
ON DUPLICATE KEY UPDATE quantity_on_hand = VALUES(quantity_on_hand), quantity_reserved = 0, low_stock_threshold = 5, version = VALUES(version), updated_at = VALUES(updated_at);
INSERT INTO inventories (variant_id, quantity_on_hand, quantity_reserved, low_stock_threshold, version, updated_at)
SELECT id, 12, 0, 5, 0, @seed_now FROM product_variants WHERE sku = 'DEMO-UGREEN-PB10K-GRAY'
ON DUPLICATE KEY UPDATE quantity_on_hand = VALUES(quantity_on_hand), quantity_reserved = 0, low_stock_threshold = 5, version = VALUES(version), updated_at = VALUES(updated_at);
INSERT INTO inventories (variant_id, quantity_on_hand, quantity_reserved, low_stock_threshold, version, updated_at)
SELECT id, 20, 0, 5, 0, @seed_now FROM product_variants WHERE sku = 'DEMO-ESR-MAGSAFE-CLEAR'
ON DUPLICATE KEY UPDATE quantity_on_hand = VALUES(quantity_on_hand), quantity_reserved = 0, low_stock_threshold = 5, version = VALUES(version), updated_at = VALUES(updated_at);
INSERT INTO inventories (variant_id, quantity_on_hand, quantity_reserved, low_stock_threshold, version, updated_at)
SELECT id, 8, 0, 5, 0, @seed_now FROM product_variants WHERE sku = 'DEMO-JBL-USBC-WHITE'
ON DUPLICATE KEY UPDATE quantity_on_hand = VALUES(quantity_on_hand), quantity_reserved = 0, low_stock_threshold = 5, version = VALUES(version), updated_at = VALUES(updated_at);

-- Demo users share the documented demo password. The BCrypt hash is not a production secret.
INSERT INTO users (email, password_hash, full_name, phone, status, email_verified, date_of_birth, created_at, updated_at) VALUES
  ('demo.admin@techstore.local', '$2a$10$0skPp2Q4.UoAz3TmVIXftec2RDfD.k8Klx6xWI7wQIUkXZhKZtEGy', 'Admin Demo', '0909000001', 'ACTIVE', TRUE, '1990-01-15', @seed_now, @seed_now),
  ('demo.manager@techstore.local', '$2a$10$0skPp2Q4.UoAz3TmVIXftec2RDfD.k8Klx6xWI7wQIUkXZhKZtEGy', 'Quản lý Demo', '0909000004', 'ACTIVE', TRUE, '1992-03-08', @seed_now, @seed_now),
  ('demo.customer1@techstore.local', '$2a$10$0skPp2Q4.UoAz3TmVIXftec2RDfD.k8Klx6xWI7wQIUkXZhKZtEGy', 'Nguyễn Demo Một', '0909000002', 'ACTIVE', TRUE, '1995-05-20', @seed_now, @seed_now),
  ('demo.customer2@techstore.local', '$2a$10$0skPp2Q4.UoAz3TmVIXftec2RDfD.k8Klx6xWI7wQIUkXZhKZtEGy', 'Trần Demo Hai', '0909000003', 'ACTIVE', TRUE, '1998-10-11', @seed_now, @seed_now)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), full_name = VALUES(full_name), phone = VALUES(phone),
                        status = 'ACTIVE', email_verified = TRUE, date_of_birth = VALUES(date_of_birth), updated_at = VALUES(updated_at);

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE (u.email IN ('demo.admin@techstore.local', 'demo.manager@techstore.local') AND r.code = 'ADMIN')
   OR (u.email IN ('demo.customer1@techstore.local', 'demo.customer2@techstore.local') AND r.code = 'CUSTOMER');

SET @demo_admin_id := (SELECT id FROM users WHERE email = 'demo.admin@techstore.local');
SET @demo_customer1_id := (SELECT id FROM users WHERE email = 'demo.customer1@techstore.local');
SET @demo_customer2_id := (SELECT id FROM users WHERE email = 'demo.customer2@techstore.local');

DELETE it FROM inventory_transactions it
JOIN inventories i ON i.id = it.inventory_id
JOIN product_variants v ON v.id = i.variant_id
WHERE v.sku LIKE 'DEMO-%' AND it.note = @seed_note;

INSERT INTO inventory_transactions (inventory_id, transaction_type, quantity_change, reference_type, reference_id, note, created_by, created_at)
SELECT i.id, 'IMPORT', i.quantity_on_hand, 'DEMO_SEED', NULL, @seed_note, @demo_admin_id, @seed_now
FROM inventories i JOIN product_variants v ON v.id = i.variant_id
WHERE v.sku LIKE 'DEMO-%' AND i.quantity_on_hand > 0;

-- Five orders cover every lifecycle state accepted by the administration UI.
INSERT INTO orders (order_number, user_id, status, payment_method, payment_status, subtotal, discount_amount, shipping_fee, total_amount, note, placed_at, updated_at, cancelled_at, internal_note) VALUES
  ('DEMO-2026-0001', @demo_customer1_id, 'PENDING', 'COD', 'UNPAID', 428000, 0, 30000, 458000, 'Đơn demo chờ xác nhận.', DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY), NULL, 'US-15.6 demo seed'),
  ('DEMO-2026-0002', @demo_customer1_id, 'CONFIRMED', 'BANK_TRANSFER', 'PAID', 129000, 0, 0, 129000, 'Đơn demo đã xác nhận.', DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 2 DAY), NULL, 'US-15.6 demo seed'),
  ('DEMO-2026-0003', @demo_customer2_id, 'SHIPPING', 'COD', 'UNPAID', 499000, 0, 30000, 529000, 'Đơn demo đang giao.', DATE_SUB(@seed_now, INTERVAL 4 DAY), DATE_SUB(@seed_now, INTERVAL 3 DAY), NULL, 'US-15.6 demo seed'),
  ('DEMO-2026-0004', @demo_customer2_id, 'COMPLETED', 'ONLINE', 'PAID', 249000, 20000, 30000, 259000, 'Đơn demo hoàn thành, có giảm giá.', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 5 DAY), NULL, 'US-15.6 demo seed'),
  ('DEMO-2026-0005', @demo_customer1_id, 'CANCELLED', 'COD', 'REFUNDED', 349000, 0, 0, 349000, 'Đơn demo đã huỷ.', DATE_SUB(@seed_now, INTERVAL 6 DAY), DATE_SUB(@seed_now, INTERVAL 6 DAY), DATE_SUB(@seed_now, INTERVAL 6 DAY), 'US-15.6 demo seed')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), status = VALUES(status), payment_method = VALUES(payment_method),
                        payment_status = VALUES(payment_status), subtotal = VALUES(subtotal), discount_amount = VALUES(discount_amount),
                        shipping_fee = VALUES(shipping_fee), total_amount = VALUES(total_amount), note = VALUES(note),
                        placed_at = VALUES(placed_at), updated_at = VALUES(updated_at), cancelled_at = VALUES(cancelled_at), internal_note = VALUES(internal_note);

INSERT INTO order_addresses (order_id, recipient_name, recipient_phone, line1, ward, district, province)
SELECT id, 'Nguyễn Demo Một', '0909000002', '12 Đường Demo', 'Phường Nam Đồng', 'Quận Hải An', 'Hải Phòng' FROM orders WHERE order_number = 'DEMO-2026-0001'
ON DUPLICATE KEY UPDATE recipient_name = VALUES(recipient_name), recipient_phone = VALUES(recipient_phone), line1 = VALUES(line1), ward = VALUES(ward), district = VALUES(district), province = VALUES(province);
INSERT INTO order_addresses (order_id, recipient_name, recipient_phone, line1, ward, district, province)
SELECT id, 'Nguyễn Demo Một', '0909000002', '12 Đường Demo', 'Phường Nam Đồng', 'Quận Hải An', 'Hải Phòng' FROM orders WHERE order_number = 'DEMO-2026-0002'
ON DUPLICATE KEY UPDATE recipient_name = VALUES(recipient_name), recipient_phone = VALUES(recipient_phone), line1 = VALUES(line1), ward = VALUES(ward), district = VALUES(district), province = VALUES(province);
INSERT INTO order_addresses (order_id, recipient_name, recipient_phone, line1, ward, district, province)
SELECT id, 'Trần Demo Hai', '0909000003', '88 Ngõ Minh Khai', 'Phường Lê Chân', 'Quận Lê Chân', 'Hải Phòng' FROM orders WHERE order_number = 'DEMO-2026-0003'
ON DUPLICATE KEY UPDATE recipient_name = VALUES(recipient_name), recipient_phone = VALUES(recipient_phone), line1 = VALUES(line1), ward = VALUES(ward), district = VALUES(district), province = VALUES(province);
INSERT INTO order_addresses (order_id, recipient_name, recipient_phone, line1, ward, district, province)
SELECT id, 'Trần Demo Hai', '0909000003', '88 Ngõ Minh Khai', 'Phường Lê Chân', 'Quận Lê Chân', 'Hải Phòng' FROM orders WHERE order_number = 'DEMO-2026-0004'
ON DUPLICATE KEY UPDATE recipient_name = VALUES(recipient_name), recipient_phone = VALUES(recipient_phone), line1 = VALUES(line1), ward = VALUES(ward), district = VALUES(district), province = VALUES(province);
INSERT INTO order_addresses (order_id, recipient_name, recipient_phone, line1, ward, district, province)
SELECT id, 'Nguyễn Demo Một', '0909000002', '12 Đường Demo', 'Phường Nam Đồng', 'Quận Hải An', 'Hải Phòng' FROM orders WHERE order_number = 'DEMO-2026-0005'
ON DUPLICATE KEY UPDATE recipient_name = VALUES(recipient_name), recipient_phone = VALUES(recipient_phone), line1 = VALUES(line1), ward = VALUES(ward), district = VALUES(district), province = VALUES(province);

DELETE oi FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.order_number LIKE 'DEMO-2026-%';
INSERT INTO order_items (order_id, variant_id, product_name, sku, variant_label, unit_price, quantity, subtotal)
SELECT o.id, v.id, '[Demo] Anker Nano Charger 20W', v.sku, 'Trắng', 299000, 1, 299000 FROM orders o JOIN product_variants v ON v.sku = 'DEMO-ANKER-20W-WHITE' WHERE o.order_number = 'DEMO-2026-0001'
UNION ALL
SELECT o.id, v.id, '[Demo] Baseus USB-C 100W Cable', v.sku, 'Đen - 1 m', 129000, 1, 129000 FROM orders o JOIN product_variants v ON v.sku = 'DEMO-BASEUS-C100-BLACK' WHERE o.order_number = 'DEMO-2026-0001'
UNION ALL
SELECT o.id, v.id, '[Demo] Baseus USB-C 100W Cable', v.sku, 'Đen - 1 m', 129000, 1, 129000 FROM orders o JOIN product_variants v ON v.sku = 'DEMO-BASEUS-C100-BLACK' WHERE o.order_number = 'DEMO-2026-0002'
UNION ALL
SELECT o.id, v.id, '[Demo] Ugreen 10000mAh Power Bank', v.sku, 'Xám - 10000 mAh', 499000, 1, 499000 FROM orders o JOIN product_variants v ON v.sku = 'DEMO-UGREEN-PB10K-GRAY' WHERE o.order_number = 'DEMO-2026-0003'
UNION ALL
SELECT o.id, v.id, '[Demo] ESR Classic MagSafe Case', v.sku, 'Trong suốt - iPhone 15', 249000, 1, 249000 FROM orders o JOIN product_variants v ON v.sku = 'DEMO-ESR-MAGSAFE-CLEAR' WHERE o.order_number = 'DEMO-2026-0004'
UNION ALL
SELECT o.id, v.id, '[Demo] JBL Tune USB-C Earphones', v.sku, 'Trắng', 349000, 1, 349000 FROM orders o JOIN product_variants v ON v.sku = 'DEMO-JBL-USBC-WHITE' WHERE o.order_number = 'DEMO-2026-0005';

DELETE osh FROM order_status_history osh JOIN orders o ON o.id = osh.order_id WHERE o.order_number LIKE 'DEMO-2026-%';
INSERT INTO order_status_history (order_id, status, changed_at, changed_by)
SELECT id, 'PENDING', DATE_SUB(@seed_now, INTERVAL 1 DAY), @demo_customer1_id FROM orders WHERE order_number = 'DEMO-2026-0001'
UNION ALL SELECT id, 'PENDING', DATE_SUB(@seed_now, INTERVAL 2 DAY), @demo_customer1_id FROM orders WHERE order_number = 'DEMO-2026-0002'
UNION ALL SELECT id, 'CONFIRMED', DATE_SUB(@seed_now, INTERVAL 2 DAY) + INTERVAL 2 HOUR, @demo_admin_id FROM orders WHERE order_number = 'DEMO-2026-0002'
UNION ALL SELECT id, 'PENDING', DATE_SUB(@seed_now, INTERVAL 4 DAY), @demo_customer2_id FROM orders WHERE order_number = 'DEMO-2026-0003'
UNION ALL SELECT id, 'CONFIRMED', DATE_SUB(@seed_now, INTERVAL 4 DAY) + INTERVAL 3 HOUR, @demo_admin_id FROM orders WHERE order_number = 'DEMO-2026-0003'
UNION ALL SELECT id, 'SHIPPING', DATE_SUB(@seed_now, INTERVAL 3 DAY), @demo_admin_id FROM orders WHERE order_number = 'DEMO-2026-0003'
UNION ALL SELECT id, 'PENDING', DATE_SUB(@seed_now, INTERVAL 8 DAY), @demo_customer2_id FROM orders WHERE order_number = 'DEMO-2026-0004'
UNION ALL SELECT id, 'CONFIRMED', DATE_SUB(@seed_now, INTERVAL 8 DAY) + INTERVAL 2 HOUR, @demo_admin_id FROM orders WHERE order_number = 'DEMO-2026-0004'
UNION ALL SELECT id, 'SHIPPING', DATE_SUB(@seed_now, INTERVAL 7 DAY), @demo_admin_id FROM orders WHERE order_number = 'DEMO-2026-0004'
UNION ALL SELECT id, 'COMPLETED', DATE_SUB(@seed_now, INTERVAL 5 DAY), @demo_admin_id FROM orders WHERE order_number = 'DEMO-2026-0004'
UNION ALL SELECT id, 'PENDING', DATE_SUB(@seed_now, INTERVAL 6 DAY), @demo_customer1_id FROM orders WHERE order_number = 'DEMO-2026-0005'
UNION ALL SELECT id, 'CANCELLED', DATE_SUB(@seed_now, INTERVAL 6 DAY) + INTERVAL 1 HOUR, @demo_admin_id FROM orders WHERE order_number = 'DEMO-2026-0005';

COMMIT;

-- Expected summary after a successful run: 6 variants, 5 products, 4 demo users
-- and 5 DEMO-2026-* orders covering PENDING, CONFIRMED, SHIPPING, COMPLETED,
-- and CANCELLED.
