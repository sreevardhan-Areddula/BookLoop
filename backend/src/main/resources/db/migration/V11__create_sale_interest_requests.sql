CREATE TABLE sale_interest_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    listing_id BINARY(16) NOT NULL,
    requester_id BINARY(16) NOT NULL,
    owner_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reserve_listing BOOLEAN NULL,
    owner_viewed_at DATETIME(6) NULL,
    accepted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_sale_interest_listing FOREIGN KEY (listing_id) REFERENCES book_listings (id),
    CONSTRAINT fk_sale_interest_requester FOREIGN KEY (requester_id) REFERENCES accounts (id),
    CONSTRAINT fk_sale_interest_owner FOREIGN KEY (owner_id) REFERENCES accounts (id),
    INDEX idx_sale_interest_requester_status (requester_id, status, created_at),
    INDEX idx_sale_interest_owner_status (owner_id, status, created_at),
    INDEX idx_sale_interest_listing_status (listing_id, status)
) ENGINE=InnoDB;
