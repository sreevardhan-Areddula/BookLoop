CREATE TABLE email_verification_tokens (
    id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_verification_token_account
        FOREIGN KEY (account_id) REFERENCES accounts (id)
        ON DELETE CASCADE,
    INDEX idx_verification_account (account_id)
) ENGINE=InnoDB;
