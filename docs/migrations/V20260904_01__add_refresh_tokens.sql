-- TechStore manual migration
-- Story: US-01.3 — logout and server-side refresh-token revocation
-- Run once against an existing MySQL 8.0.16+ `techstore` database.

USE techstore;

-- Store only the signed JWT identifier (`jti`), never the raw refresh token.
CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token_id        CHAR(36) NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    revoked_at      TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_token_id UNIQUE (token_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
