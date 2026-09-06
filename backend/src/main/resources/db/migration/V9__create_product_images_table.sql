-- V9: Create product_images table
CREATE TABLE product_images (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT       NOT NULL,
    variant_id    BIGINT       NULL,
    image_url     VARCHAR(500) NOT NULL,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_images_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE SET NULL
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);
CREATE INDEX idx_product_images_variant_id ON product_images (variant_id);
CREATE INDEX idx_product_images_is_primary ON product_images (is_primary);
