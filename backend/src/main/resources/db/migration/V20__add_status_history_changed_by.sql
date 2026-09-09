ALTER TABLE order_status_history
    ADD COLUMN changed_by BIGINT NULL;

ALTER TABLE order_status_history
    ADD CONSTRAINT fk_order_status_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_order_status_history_changed_by
    ON order_status_history(changed_by);
