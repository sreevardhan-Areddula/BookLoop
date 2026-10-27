CREATE TABLE refresh_sessions (
    id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    family_id BINARY(16) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    replaced_by_hash CHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_account
        FOREIGN KEY (account_id) REFERENCES accounts (id)
        ON DELETE CASCADE,
    INDEX idx_refresh_family (family_id),
    INDEX idx_refresh_account (account_id)
) ENGINE=InnoDB;
