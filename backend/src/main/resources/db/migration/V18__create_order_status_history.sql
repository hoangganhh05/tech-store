CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(25) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_order_status_history_order_changed_at
    ON order_status_history(order_id, changed_at);

INSERT INTO order_status_history (order_id, status, changed_at)
SELECT id, status, placed_at
FROM orders;
