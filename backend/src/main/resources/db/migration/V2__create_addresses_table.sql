-- V2: US-01.7 — Create addresses table for shipping address management

CREATE TABLE addresses (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    province       VARCHAR(100) NOT NULL,
    district       VARCHAR(100) NOT NULL,
    ward           VARCHAR(100) NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);

