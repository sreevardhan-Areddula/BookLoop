CREATE TABLE password_reset_tokens (
    id BINARY(16) NOT NULL PRIMARY KEY,
    account_id BINARY(16) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_reset_token_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT uk_reset_token_hash UNIQUE (token_hash),
    INDEX idx_reset_account (account_id)
);
