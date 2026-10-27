package com.bookloop.book;

import com.bookloop.account.Account;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "book_listings", indexes = {
        @Index(name = "idx_book_title", columnList = "title"),
        @Index(name = "idx_book_city", columnList = "city"),
        @Index(name = "idx_book_owner", columnList = "owner_id")
})
public class BookListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @ElementCollection
    @CollectionTable(name = "book_listing_genres", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "genre_position")
    @Column(name = "genre", nullable = false, length = 80)
    private List<String> genres = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition", nullable = false, length = 20)
    private BookCondition condition;

    @Column(nullable = false, length = 255)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false, length = 20)
    private BookOfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", nullable = false, length = 20)
    private ListingAvailability availability = ListingAvailability.AVAILABLE;

    @Column(name = "asking_price", precision = 10, scale = 2)
    private BigDecimal askingPrice;

    @Column(name = "sale_price_verified", nullable = false)
    private boolean salePriceVerified;

    @Column(name = "sale_bill_document_id")
    private UUID saleBillDocumentId;

    @Column(length = 20)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BookListing() {
    }

    public BookListing(Account owner, String title, String author, List<String> genres,
                       BookCondition condition, String city, BookOfferType offerType,
                       BigDecimal askingPrice, String isbn, String description) {
        this.owner = owner;
        this.title = title;
        this.author = author;
        this.genres = new ArrayList<>(genres);
        this.condition = condition;
        this.city = city;
        this.offerType = offerType;
        this.askingPrice = askingPrice;
        this.isbn = isbn;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Account getOwner() { return owner; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public List<String> getGenres() { return List.copyOf(genres); }
    public BookCondition getCondition() { return condition; }
    public String getCity() { return city; }
    public BookOfferType getOfferType() { return offerType; }
    public ListingAvailability getAvailability() { return availability; }
    /** Changes whether this listing can be requested in a new exchange. */
    public void setAvailability(ListingAvailability availability) { this.availability = availability; }
    public BigDecimal getAskingPrice() { return askingPrice; }
    public boolean isSalePriceVerified() { return salePriceVerified; }
    public UUID getSaleBillDocumentId() { return saleBillDocumentId; }
    /** Attaches a previously OCR-checked private bill and marks this sale price as checked. */
    public void markSalePriceVerified(UUID billDocumentId) {
        this.saleBillDocumentId = billDocumentId;
        this.salePriceVerified = true;
    }
    public String getIsbn() { return isbn; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
