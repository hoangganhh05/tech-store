-- V8: Create product_variants table
CREATE TABLE product_variants (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT         NOT NULL,
    sku            VARCHAR(100)   NOT NULL,
    color          VARCHAR(50),
    storage        VARCHAR(50),
    price          DECIMAL(15, 2) NOT NULL,
    original_price DECIMAL(15, 2),
    stock_quantity INT            NOT NULL DEFAULT 0,
    status         VARCHAR(30)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku)
);

CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_product_variants_sku ON product_variants (sku);
CREATE INDEX idx_product_variants_status ON product_variants (status);
