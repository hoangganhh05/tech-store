-- US-14.5: indexes for the active storefront product path and batched detail data.
CREATE INDEX idx_products_storefront_status_deleted_created
    ON products (status, is_deleted, created_at, id);

CREATE INDEX idx_products_category_status_deleted
    ON products (category_id, status, is_deleted, created_at);

CREATE INDEX idx_products_brand_status_deleted
    ON products (brand_id, status, is_deleted, created_at);

CREATE INDEX idx_products_name
    ON products (name);

CREATE INDEX idx_product_variants_product_deleted_status_created
    ON product_variants (product_id, is_deleted, status, created_at);

CREATE INDEX idx_product_images_product_primary_order
    ON product_images (product_id, is_primary, display_order, id);

CREATE INDEX idx_inventory_transactions_inventory_type
    ON inventory_transactions (inventory_id, transaction_type);

CREATE INDEX idx_categories_parent_active_order
    ON categories (parent_id, is_active, display_order, name);
