ALTER TABLE book_listings
    ADD COLUMN sale_price_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sale_bill_document_id BINARY(16) NULL,
    ADD CONSTRAINT fk_book_listing_sale_bill
        FOREIGN KEY (sale_bill_document_id) REFERENCES sale_bill_documents (id);
