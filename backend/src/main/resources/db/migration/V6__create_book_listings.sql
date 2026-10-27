CREATE TABLE book_listings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    book_condition VARCHAR(20) NOT NULL,
    city VARCHAR(255) NOT NULL,
    offer_type VARCHAR(20) NOT NULL,
    asking_price DECIMAL(10,2) NULL,
    isbn VARCHAR(20) NULL,
    description TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_book_listing_owner FOREIGN KEY (owner_id) REFERENCES accounts (id),
    INDEX idx_book_title (title),
    INDEX idx_book_city (city),
    INDEX idx_book_owner (owner_id)
);

CREATE TABLE book_listing_genres (
    listing_id BINARY(16) NOT NULL,
    genre_position INT NOT NULL,
    genre VARCHAR(80) NOT NULL,
    PRIMARY KEY (listing_id, genre_position),
    CONSTRAINT fk_listing_genre_book FOREIGN KEY (listing_id) REFERENCES book_listings (id) ON DELETE CASCADE
);
