-- TechStore database schema
-- Story: US-00.3 / Tasks: T-00.3.1, T-00.3.2, T-00.3.3
-- Target database: PostgreSQL 15+

BEGIN;

CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE addresses (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_name  VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    line1           VARCHAR(255) NOT NULL,
    line2           VARCHAR(255),
    ward            VARCHAR(120),
    district        VARCHAR(120) NOT NULL,
    province        VARCHAR(120) NOT NULL,
    postal_code     VARCHAR(20),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_addresses_one_default_per_user
    ON addresses(user_id) WHERE is_default = TRUE;

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT REFERENCES categories(id) ON DELETE RESTRICT,
    name            VARCHAR(150) NOT NULL,
    slug            VARCHAR(180) NOT NULL UNIQUE,
    display_order   INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    is_visible      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_categories_not_self_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE TABLE brands (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL UNIQUE,
    slug            VARCHAR(180) NOT NULL UNIQUE,
    logo_url        VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    brand_id        BIGINT NOT NULL REFERENCES brands(id) ON DELETE RESTRICT,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(280) NOT NULL UNIQUE,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'ACTIVE', 'HIDDEN', 'DISCONTINUED')),
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_variants (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku             VARCHAR(80) NOT NULL UNIQUE,
    color           VARCHAR(80),
    storage         VARCHAR(80),
    price           NUMERIC(15,2) NOT NULL CHECK (price >= 0),
    compare_at_price NUMERIC(15,2) CHECK (compare_at_price IS NULL OR compare_at_price >= price),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'HIDDEN', 'DISCONTINUED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_variant_options UNIQUE (product_id, color, storage)
);

CREATE TABLE product_images (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id      BIGINT REFERENCES product_variants(id) ON DELETE CASCADE,
    image_url       VARCHAR(500) NOT NULL,
    alt_text        VARCHAR(255),
    display_order   INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_product_primary_image
    ON product_images(product_id) WHERE is_primary = TRUE AND variant_id IS NULL;
CREATE UNIQUE INDEX uq_variant_primary_image
    ON product_images(variant_id) WHERE is_primary = TRUE AND variant_id IS NOT NULL;

CREATE TABLE product_specifications (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    spec_key        VARCHAR(100) NOT NULL,
    spec_value      VARCHAR(500) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    CONSTRAINT uq_product_spec_key UNIQUE (product_id, spec_key)
);

CREATE TABLE inventories (
    id              BIGSERIAL PRIMARY KEY,
    variant_id      BIGINT NOT NULL UNIQUE REFERENCES product_variants(id) ON DELETE RESTRICT,
    quantity_on_hand INTEGER NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved INTEGER NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inventory_reservation CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE TABLE inventory_transactions (
    id              BIGSERIAL PRIMARY KEY,
    inventory_id    BIGINT NOT NULL REFERENCES inventories(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(20) NOT NULL
                    CHECK (transaction_type IN ('IMPORT', 'SALE', 'CANCEL_RETURN', 'ADJUSTMENT', 'RESERVE', 'RELEASE')),
    quantity_change INTEGER NOT NULL CHECK (quantity_change <> 0),
    reference_type  VARCHAR(30),
    reference_id    BIGINT,
    note            VARCHAR(500),
    created_by      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE carts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    session_key     VARCHAR(100) UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'CONVERTED', 'ABANDONED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_cart_owner CHECK (user_id IS NOT NULL OR session_key IS NOT NULL)
);

CREATE TABLE cart_items (
    id              BIGSERIAL PRIMARY KEY,
    cart_id         BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    variant_id      BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE RESTRICT,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_id)
);

CREATE TABLE vouchers (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    discount_type   VARCHAR(15) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value  NUMERIC(15,2) NOT NULL CHECK (discount_value > 0),
    max_discount    NUMERIC(15,2) CHECK (max_discount IS NULL OR max_discount >= 0),
    minimum_order   NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (minimum_order >= 0),
    usage_limit     INTEGER CHECK (usage_limit IS NULL OR usage_limit > 0),
    per_user_limit  INTEGER NOT NULL DEFAULT 1 CHECK (per_user_limit > 0),
    used_count      INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_voucher_period CHECK (ends_at > starts_at),
    CONSTRAINT ck_percent_discount CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),
    CONSTRAINT ck_voucher_usage CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(40) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    voucher_id      BIGINT REFERENCES vouchers(id) ON DELETE SET NULL,
    status          VARCHAR(25) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED')),
    payment_method  VARCHAR(20) NOT NULL CHECK (payment_method IN ('COD', 'BANK_TRANSFER', 'ONLINE')),
    payment_status  VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
                    CHECK (payment_status IN ('UNPAID', 'PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    subtotal        NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0),
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    shipping_fee    NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
    total_amount    NUMERIC(15,2) NOT NULL CHECK (total_amount >= 0),
    note            VARCHAR(500),
    placed_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at    TIMESTAMPTZ,
    CONSTRAINT ck_order_total CHECK (total_amount = subtotal - discount_amount + shipping_fee),
    CONSTRAINT ck_order_discount CHECK (discount_amount <= subtotal)
);

-- Immutable shipping snapshot: later address edits must not change an existing order.
CREATE TABLE order_addresses (
    order_id        BIGINT PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
    recipient_name  VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    line1           VARCHAR(255) NOT NULL,
    line2           VARCHAR(255),
    ward            VARCHAR(120),
    district        VARCHAR(120) NOT NULL,
    province        VARCHAR(120) NOT NULL,
    postal_code     VARCHAR(20)
);

CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id      BIGINT NOT NULL REFERENCES product_variants(id) ON DELETE RESTRICT,
    product_name    VARCHAR(255) NOT NULL,
    sku             VARCHAR(80) NOT NULL,
    variant_label   VARCHAR(180),
    unit_price      NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    line_total      NUMERIC(15,2) NOT NULL CHECK (line_total >= 0),
    CONSTRAINT uq_order_variant UNIQUE (order_id, variant_id),
    CONSTRAINT ck_order_item_total CHECK (line_total = unit_price * quantity)
);

CREATE TABLE order_status_history (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status     VARCHAR(25),
    to_status       VARCHAR(25) NOT NULL,
    changed_by      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    note            VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE voucher_usages (
    id              BIGSERIAL PRIMARY KEY,
    voucher_id      BIGINT NOT NULL REFERENCES vouchers(id) ON DELETE RESTRICT,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    order_id        BIGINT NOT NULL UNIQUE REFERENCES orders(id) ON DELETE RESTRICT,
    discount_amount NUMERIC(15,2) NOT NULL CHECK (discount_amount >= 0),
    used_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    order_item_id   BIGINT UNIQUE REFERENCES order_items(id) ON DELETE SET NULL,
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title           VARCHAR(150),
    content         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'PUBLISHED', 'HIDDEN')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE wishlists (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_inventory_transactions_inventory ON inventory_transactions(inventory_id, created_at DESC);
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
CREATE INDEX idx_orders_user ON orders(user_id, placed_at DESC);
CREATE INDEX idx_orders_status ON orders(status, placed_at DESC);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_status_history_order ON order_status_history(order_id, created_at);
CREATE INDEX idx_voucher_usages_voucher_user ON voucher_usages(voucher_id, user_id);
CREATE INDEX idx_reviews_product_status ON reviews(product_id, status);
CREATE INDEX idx_wishlists_user ON wishlists(user_id);

-- Idempotent reference/sample data.
INSERT INTO roles (code, name) VALUES
    ('CUSTOMER', 'Customer'),
    ('ADMIN', 'Administrator')
ON CONFLICT (code) DO NOTHING;

INSERT INTO categories (name, slug, display_order) VALUES
    ('Điện thoại', 'dien-thoai', 1),
    ('Phụ kiện', 'phu-kien', 2)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO brands (name, slug) VALUES
    ('Apple', 'apple'),
    ('Samsung', 'samsung')
ON CONFLICT (slug) DO NOTHING;

COMMIT;
