-- V12: Create inventories and inventory_transactions tables

CREATE TABLE inventories (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    variant_id          BIGINT NOT NULL UNIQUE,
    quantity_on_hand    INTEGER NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved   INTEGER NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    version             BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inventory_reservation CHECK (quantity_reserved <= quantity_on_hand),
    CONSTRAINT fk_inventories_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE
);

CREATE TABLE inventory_transactions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id     BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL
                     CHECK (transaction_type IN ('IMPORT', 'SALE', 'CANCEL_RETURN', 'ADJUSTMENT', 'RESERVE', 'RELEASE')),
    quantity_change  INTEGER NOT NULL CHECK (quantity_change <> 0),
    reference_type   VARCHAR(30),
    reference_id     BIGINT,
    note             VARCHAR(500),
    created_by       BIGINT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_transactions_inventory FOREIGN KEY (inventory_id) REFERENCES inventories (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_transactions_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_inventories_variant ON inventories (variant_id);
CREATE INDEX idx_inventory_transactions_inventory ON inventory_transactions (inventory_id, created_at DESC);

-- Backfill initial inventory records for existing variants
INSERT INTO inventories (variant_id, quantity_on_hand, quantity_reserved, low_stock_threshold, version, updated_at)
SELECT id, stock_quantity, 0, 5, 0, CURRENT_TIMESTAMP
FROM product_variants;
