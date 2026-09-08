CREATE TABLE order_addresses (
    order_id BIGINT PRIMARY KEY,
    recipient_name VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    ward VARCHAR(120),
    district VARCHAR(120) NOT NULL,
    province VARCHAR(120) NOT NULL,
    CONSTRAINT fk_order_addresses_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(80) NOT NULL,
    variant_label VARCHAR(180),
    unit_price NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    subtotal NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
