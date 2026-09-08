-- US-08.2: persist the selected method on orders. Cart submission belongs to US-08.4.
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED')),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('COD', 'BANK_TRANSFER', 'ONLINE')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
        CHECK (payment_status IN ('UNPAID', 'PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    subtotal NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0),
    discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    shipping_fee NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
    total_amount NUMERIC(15,2) NOT NULL CHECK (total_amount >= 0),
    note VARCHAR(500),
    placed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT ck_order_total CHECK (total_amount = subtotal - discount_amount + shipping_fee),
    CONSTRAINT ck_order_discount CHECK (discount_amount <= subtotal),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);
