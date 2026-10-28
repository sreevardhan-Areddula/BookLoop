CREATE TABLE free_claim_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    listing_id BINARY(16) NOT NULL,
    requester_id BINARY(16) NOT NULL,
    owner_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_viewed_at DATETIME(6) NULL,
    accepted_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    cancelled_by_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_free_claim_listing FOREIGN KEY (listing_id) REFERENCES book_listings (id),
    CONSTRAINT fk_free_claim_requester FOREIGN KEY (requester_id) REFERENCES accounts (id),
    CONSTRAINT fk_free_claim_owner FOREIGN KEY (owner_id) REFERENCES accounts (id),
    CONSTRAINT fk_free_claim_cancelled_by FOREIGN KEY (cancelled_by_id) REFERENCES accounts (id),
    INDEX idx_free_claim_requester_status (requester_id, status, created_at),
    INDEX idx_free_claim_owner_status (owner_id, status, created_at),
    INDEX idx_free_claim_listing_status (listing_id, status)
) ENGINE=InnoDB;

CREATE TABLE free_claim_messages (
    id BINARY(16) NOT NULL PRIMARY KEY,
    claim_request_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    message_text VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    CONSTRAINT fk_free_claim_message_request FOREIGN KEY (claim_request_id) REFERENCES free_claim_requests (id),
    CONSTRAINT fk_free_claim_message_sender FOREIGN KEY (sender_id) REFERENCES accounts (id),
    CONSTRAINT fk_free_claim_message_recipient FOREIGN KEY (recipient_id) REFERENCES accounts (id),
    INDEX idx_free_claim_message_history (claim_request_id, created_at),
    INDEX idx_free_claim_message_unread (recipient_id, read_at)
) ENGINE=InnoDB;
