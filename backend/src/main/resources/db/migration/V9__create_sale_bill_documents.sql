CREATE TABLE sale_bill_documents (
    id BINARY(16) NOT NULL PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data MEDIUMBLOB NOT NULL,
    check_status VARCHAR(32) NOT NULL,
    detected_paid_amount DECIMAL(10,2) NULL,
    created_at DATETIME(6) NOT NULL,
    checked_at DATETIME(6) NULL,
    CONSTRAINT fk_sale_bill_owner FOREIGN KEY (owner_id) REFERENCES accounts (id),
    INDEX idx_sale_bill_owner (owner_id, created_at)
) ENGINE=InnoDB;
