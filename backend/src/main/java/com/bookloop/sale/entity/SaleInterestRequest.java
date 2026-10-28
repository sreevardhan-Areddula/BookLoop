package com.bookloop.sale.entity;



import com.bookloop.account.entity.Account;
import com.bookloop.book.entity.BookListing;
import jakarta.persistence.Column;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sale_interest_requests", indexes = {
        @Index(name = "idx_sale_interest_requester_status", columnList = "requester_id,status,created_at"),
        @Index(name = "idx_sale_interest_owner_status", columnList = "owner_id,status,created_at"),
        @Index(name = "idx_sale_interest_listing_status", columnList = "listing_id,status")
})
public class SaleInterestRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private BookListing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Account requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleInterestStatus status = SaleInterestStatus.PENDING;

    @Column(name = "reserve_listing")
    private Boolean reserveListing;

    @Column(name = "owner_viewed_at")
    private Instant ownerViewedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SaleInterestRequest() { }

    /** Creates an unread pending interest request for one sale listing. */
    public SaleInterestRequest(BookListing listing, Account requester, Account owner, Instant now) {
        this.listing = listing;
        this.requester = requester;
        this.owner = owner;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Records the seller's decision and optional listing reservation. */
    public void accept(boolean reserveListing, Instant now) {
        requirePending();
        this.status = SaleInterestStatus.ACCEPTED;
        this.reserveListing = reserveListing;
        this.acceptedAt = now;
    }

    /** Declines a request without changing listing availability. */
    public void decline() {
        requirePending();
        this.status = SaleInterestStatus.DECLINED;
    }

    /** Closes a pending interest because the seller reserved the listing for another request. */
    public void close() {
        requirePending();
        this.status = SaleInterestStatus.CLOSED;
    }

    /** Marks a pending incoming request as seen by its seller. */
    public void markOwnerViewed(Instant now) {
        if (ownerViewedAt == null) ownerViewedAt = now;
    }

    private void requirePending() {
        if (status != SaleInterestStatus.PENDING) {
            throw new IllegalArgumentException("This interest request is no longer pending.");
        }
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public BookListing getListing() { return listing; }
    public Account getRequester() { return requester; }
    public Account getOwner() { return owner; }
    public SaleInterestStatus getStatus() { return status; }
    public Boolean getReserveListing() { return reserveListing; }
    public Instant getOwnerViewedAt() { return ownerViewedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
