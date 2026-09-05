-- TechStore database schema
-- Story: US-00.3 / Tasks: T-00.3.1, T-00.3.2, T-00.3.3
-- Target database: MySQL 8.0.16+ (validated with MySQL Server 8.4)

CREATE DATABASE IF NOT EXISTS techstore
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE techstore;

SET NAMES utf8mb4;
SET default_storage_engine = InnoDB;
START TRANSACTION;

CREATE TABLE roles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

-- A refresh token is identified by the signed JWT `jti`; never persist the raw JWT.
CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_id        CHAR(36) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    revoked_at      TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_token_id UNIQUE (token_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Store only a SHA-256 hash of the random reset token; never persist or log its raw value.
CREATE TABLE password_reset_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_hash      CHAR(64) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    used_at         TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE addresses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    recipient_name  VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    line1           VARCHAR(255) NOT NULL,
    line2           VARCHAR(255),
    ward            VARCHAR(120),
    district        VARCHAR(120) NOT NULL,
    province        VARCHAR(120) NOT NULL,
    postal_code     VARCHAR(20),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    default_user_id BIGINT GENERATED ALWAYS AS
                    (CASE WHEN is_default = TRUE THEN user_id ELSE NULL END) STORED,
    CONSTRAINT uq_addresses_one_default_per_user UNIQUE (default_user_id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id       BIGINT,
    name            VARCHAR(150) NOT NULL,
    slug            VARCHAR(180) NOT NULL UNIQUE,
    display_order   INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    is_visible      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_categories_not_self_parent CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE TABLE brands (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL UNIQUE,
    slug            VARCHAR(180) NOT NULL UNIQUE,
    logo_url        VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     BIGINT NOT NULL,
    brand_id        BIGINT NOT NULL,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(280) NOT NULL UNIQUE,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'ACTIVE', 'HIDDEN', 'DISCONTINUED')),
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE RESTRICT
);

CREATE TABLE product_variants (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    sku             VARCHAR(80) NOT NULL UNIQUE,
    color           VARCHAR(80),
    storage         VARCHAR(80),
    price           NUMERIC(15,2) NOT NULL CHECK (price >= 0),
    compare_at_price NUMERIC(15,2) CHECK (compare_at_price IS NULL OR compare_at_price >= price),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'HIDDEN', 'DISCONTINUED')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_variant_options UNIQUE (product_id, color, storage),
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE product_images (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    variant_id      BIGINT,
    image_url       VARCHAR(500) NOT NULL,
    alt_text        VARCHAR(255),
    display_order   INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    primary_product_id BIGINT GENERATED ALWAYS AS
                    (CASE WHEN is_primary = TRUE AND variant_id IS NULL THEN product_id ELSE NULL END) STORED,
    primary_variant_id BIGINT GENERATED ALWAYS AS
                    (CASE WHEN is_primary = TRUE AND variant_id IS NOT NULL THEN variant_id ELSE NULL END) STORED,
    CONSTRAINT uq_product_primary_image UNIQUE (primary_product_id),
    CONSTRAINT uq_variant_primary_image UNIQUE (primary_variant_id),
    CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_images_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);

CREATE TABLE product_specifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    spec_key        VARCHAR(100) NOT NULL,
    spec_value      VARCHAR(500) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    CONSTRAINT uq_product_spec_key UNIQUE (product_id, spec_key),
    CONSTRAINT fk_specifications_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE inventories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    variant_id      BIGINT NOT NULL UNIQUE,
    quantity_on_hand INTEGER NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved INTEGER NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inventory_reservation CHECK (quantity_reserved <= quantity_on_hand),
    CONSTRAINT fk_inventories_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT
);

CREATE TABLE inventory_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id    BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL
                    CHECK (transaction_type IN ('IMPORT', 'SALE', 'CANCEL_RETURN', 'ADJUSTMENT', 'RESERVE', 'RELEASE')),
    quantity_change INTEGER NOT NULL CHECK (quantity_change <> 0),
    reference_type  VARCHAR(30),
    reference_id    BIGINT,
    note            VARCHAR(500),
    created_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_transactions_inventory FOREIGN KEY (inventory_id) REFERENCES inventories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_transactions_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE carts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNIQUE,
    session_key     VARCHAR(100) UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'CONVERTED', 'ABANDONED')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_cart_owner CHECK (user_id IS NOT NULL OR session_key IS NOT NULL),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id         BIGINT NOT NULL,
    variant_id      BIGINT NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT
);

CREATE TABLE vouchers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    discount_type   VARCHAR(15) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value  NUMERIC(15,2) NOT NULL CHECK (discount_value > 0),
    max_discount    NUMERIC(15,2) CHECK (max_discount IS NULL OR max_discount >= 0),
    minimum_order   NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (minimum_order >= 0),
    usage_limit     INTEGER CHECK (usage_limit IS NULL OR usage_limit > 0),
    per_user_limit  INTEGER NOT NULL DEFAULT 1 CHECK (per_user_limit > 0),
    used_count      INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    starts_at       TIMESTAMP NOT NULL,
    ends_at         TIMESTAMP NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_voucher_period CHECK (ends_at > starts_at),
    CONSTRAINT ck_percent_discount CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),
    CONSTRAINT ck_voucher_usage CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number    VARCHAR(40) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    voucher_id      BIGINT,
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
    placed_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at    TIMESTAMP,
    CONSTRAINT ck_order_total CHECK (total_amount = subtotal - discount_amount + shipping_fee),
    CONSTRAINT ck_order_discount CHECK (discount_amount <= subtotal),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL
);

-- Immutable shipping snapshot: later address edits must not change an existing order.
CREATE TABLE order_addresses (
    order_id        BIGINT PRIMARY KEY,
    recipient_name  VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    line1           VARCHAR(255) NOT NULL,
    line2           VARCHAR(255),
    ward            VARCHAR(120),
    district        VARCHAR(120) NOT NULL,
    province        VARCHAR(120) NOT NULL,
    postal_code     VARCHAR(20),
    CONSTRAINT fk_order_addresses_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE order_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    variant_id      BIGINT NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    sku             VARCHAR(80) NOT NULL,
    variant_label   VARCHAR(180),
    unit_price      NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    line_total      NUMERIC(15,2) NOT NULL CHECK (line_total >= 0),
    CONSTRAINT uq_order_variant UNIQUE (order_id, variant_id),
    CONSTRAINT ck_order_item_total CHECK (line_total = unit_price * quantity),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT
);

CREATE TABLE order_status_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    from_status     VARCHAR(25),
    to_status       VARCHAR(25) NOT NULL,
    changed_by      BIGINT,
    note            VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_history_user FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE voucher_usages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    order_id        BIGINT NOT NULL UNIQUE,
    discount_amount NUMERIC(15,2) NOT NULL CHECK (discount_amount >= 0),
    used_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_voucher_usages_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_voucher_usages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_voucher_usages_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT
);

CREATE TABLE reviews (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    order_item_id   BIGINT UNIQUE,
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title           VARCHAR(150),
    content         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'PUBLISHED', 'HIDDEN')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE SET NULL
);

CREATE TABLE wishlists (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlists_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
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
INSERT IGNORE INTO roles (code, name) VALUES
    ('CUSTOMER', 'Customer'),
    ('ADMIN', 'Administrator');

INSERT IGNORE INTO categories (name, slug, display_order) VALUES
    ('Điện thoại', 'dien-thoai', 1),
    ('Phụ kiện', 'phu-kien', 2);

INSERT IGNORE INTO brands (name, slug) VALUES
    ('Apple', 'apple'),
    ('Samsung', 'samsung');

COMMIT;
