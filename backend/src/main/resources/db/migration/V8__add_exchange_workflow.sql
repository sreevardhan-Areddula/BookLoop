ALTER TABLE book_listings
    ADD COLUMN availability VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

CREATE INDEX idx_listing_availability ON book_listings (availability);

ALTER TABLE accounts
    ADD COLUMN last_active_at DATETIME(6) NULL;

CREATE TABLE exchange_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    requester_id BINARY(16) NOT NULL,
    owner_id BINARY(16) NOT NULL,
    requested_listing_id BINARY(16) NOT NULL,
    offered_listing_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_viewed_at DATETIME(6) NULL,
    expires_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    cancelled_by_id BINARY(16) NULL,
    requester_completed_at DATETIME(6) NULL,
    owner_completed_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_exchange_requester FOREIGN KEY (requester_id) REFERENCES accounts (id),
    CONSTRAINT fk_exchange_owner FOREIGN KEY (owner_id) REFERENCES accounts (id),
    CONSTRAINT fk_exchange_requested_book FOREIGN KEY (requested_listing_id) REFERENCES book_listings (id),
    CONSTRAINT fk_exchange_offered_book FOREIGN KEY (offered_listing_id) REFERENCES book_listings (id),
    CONSTRAINT fk_exchange_cancelled_by FOREIGN KEY (cancelled_by_id) REFERENCES accounts (id),
    INDEX idx_exchange_requester_status (requester_id, status, created_at),
    INDEX idx_exchange_owner_status (owner_id, status, created_at),
    INDEX idx_exchange_requested_status (requested_listing_id, status),
    INDEX idx_exchange_offered_status (offered_listing_id, status),
    INDEX idx_exchange_expiry (status, expires_at)
) ENGINE=InnoDB;

CREATE TABLE exchange_messages (
    id BINARY(16) NOT NULL PRIMARY KEY,
    exchange_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    message_text VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    CONSTRAINT fk_exchange_message_exchange FOREIGN KEY (exchange_id) REFERENCES exchange_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_exchange_message_sender FOREIGN KEY (sender_id) REFERENCES accounts (id),
    CONSTRAINT fk_exchange_message_recipient FOREIGN KEY (recipient_id) REFERENCES accounts (id),
    INDEX idx_exchange_message_poll (exchange_id, created_at),
    INDEX idx_exchange_message_unread (recipient_id, read_at)
) ENGINE=InnoDB;
