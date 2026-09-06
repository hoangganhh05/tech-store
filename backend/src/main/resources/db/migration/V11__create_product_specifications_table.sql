-- V11: Create product_specifications table
CREATE TABLE product_specifications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT        NOT NULL,
    spec_key      VARCHAR(100)  NOT NULL,
    spec_value    VARCHAR(500)  NOT NULL,
    display_order INT           NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    CONSTRAINT fk_specs_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_specs_product_id ON product_specifications (product_id);
CREATE INDEX idx_product_specs_display_order ON product_specifications (display_order);
