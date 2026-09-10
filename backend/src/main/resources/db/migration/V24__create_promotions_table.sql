CREATE TABLE promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('PRODUCT', 'VARIANT', 'CATEGORY')),
    product_id BIGINT,
    variant_id BIGINT,
    category_id BIGINT,
    discount_percent NUMERIC(5,2) NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 100),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_promotion_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_promotion_target CHECK (
        (target_type = 'PRODUCT' AND product_id IS NOT NULL AND variant_id IS NULL AND category_id IS NULL) OR
        (target_type = 'VARIANT' AND variant_id IS NOT NULL AND product_id IS NULL AND category_id IS NULL) OR
        (target_type = 'CATEGORY' AND category_id IS NOT NULL AND product_id IS NULL AND variant_id IS NULL)
    ),
    CONSTRAINT fk_promotions_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_promotions_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_promotions_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_promotions_active_dates ON promotions(is_active, starts_at, ends_at);
CREATE INDEX idx_promotions_product ON promotions(product_id);
CREATE INDEX idx_promotions_variant ON promotions(variant_id);
CREATE INDEX idx_promotions_category ON promotions(category_id);
