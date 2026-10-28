CREATE TABLE sale_shared_bill_documents (
    id BINARY(16) NOT NULL PRIMARY KEY,
    interest_request_id BINARY(16) NOT NULL,
    uploader_id BINARY(16) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data MEDIUMBLOB NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_sale_shared_bill_interest FOREIGN KEY (interest_request_id) REFERENCES sale_interest_requests (id),
    CONSTRAINT fk_sale_shared_bill_uploader FOREIGN KEY (uploader_id) REFERENCES accounts (id),
    INDEX idx_sale_shared_bill_interest (interest_request_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE sale_chat_messages (
    id BINARY(16) NOT NULL PRIMARY KEY,
    interest_request_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    message_text VARCHAR(2000) NULL,
    shared_bill_document_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    CONSTRAINT fk_sale_chat_interest FOREIGN KEY (interest_request_id) REFERENCES sale_interest_requests (id),
    CONSTRAINT fk_sale_chat_sender FOREIGN KEY (sender_id) REFERENCES accounts (id),
    CONSTRAINT fk_sale_chat_recipient FOREIGN KEY (recipient_id) REFERENCES accounts (id),
    CONSTRAINT fk_sale_chat_shared_bill FOREIGN KEY (shared_bill_document_id) REFERENCES sale_shared_bill_documents (id),
    CONSTRAINT chk_sale_chat_content CHECK (
        (message_text IS NOT NULL AND CHAR_LENGTH(TRIM(message_text)) > 0)
        OR shared_bill_document_id IS NOT NULL
    ),
    INDEX idx_sale_chat_history (interest_request_id, created_at),
    INDEX idx_sale_chat_unread (recipient_id, read_at)
) ENGINE=InnoDB;
