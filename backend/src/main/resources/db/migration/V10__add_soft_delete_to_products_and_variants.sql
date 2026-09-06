-- V10: Add soft delete columns to products and product_variants tables
ALTER TABLE products ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN deleted_at TIMESTAMP NULL;
CREATE INDEX idx_products_is_deleted ON products (is_deleted);

ALTER TABLE product_variants ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product_variants ADD COLUMN deleted_at TIMESTAMP NULL;
CREATE INDEX idx_product_variants_is_deleted ON product_variants (is_deleted);
