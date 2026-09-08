CREATE TABLE vouchers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    discount_type VARCHAR(10) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value NUMERIC(15,2) NOT NULL CHECK (discount_value > 0),
    max_discount NUMERIC(15,2) CHECK (max_discount IS NULL OR max_discount >= 0),
    minimum_order NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (minimum_order >= 0),
    usage_limit INT CHECK (usage_limit IS NULL OR usage_limit > 0),
    per_user_limit INT NOT NULL DEFAULT 1 CHECK (per_user_limit > 0),
    used_count INT NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_voucher_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_percent_discount CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),
    CONSTRAINT ck_voucher_usage CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

ALTER TABLE orders ADD voucher_id BIGINT;
ALTER TABLE orders ADD CONSTRAINT fk_orders_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL;

CREATE TABLE voucher_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    discount_amount NUMERIC(15,2) NOT NULL CHECK (discount_amount >= 0),
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_voucher_usage_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
    CONSTRAINT fk_voucher_usage_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_voucher_usage_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_vouchers_active_dates ON vouchers(is_active, starts_at, ends_at);
CREATE INDEX idx_voucher_usages_user ON voucher_usages(voucher_id, user_id);
