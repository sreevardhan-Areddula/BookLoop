ALTER TABLE accounts
    ADD COLUMN pending_email VARCHAR(320) NULL;

CREATE UNIQUE INDEX uk_accounts_pending_email ON accounts (pending_email);

ALTER TABLE email_verification_tokens
    ADD COLUMN verification_email VARCHAR(320) NULL;
