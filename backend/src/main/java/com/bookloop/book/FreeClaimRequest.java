package com.bookloop.book;

import com.bookloop.account.Account;
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

/** Stores a private request to claim one member's available Free listing. */
@Entity
@Table(name = "free_claim_requests", indexes = {
        @Index(name = "idx_free_claim_requester_status", columnList = "requester_id,status,created_at"),
        @Index(name = "idx_free_claim_owner_status", columnList = "owner_id,status,created_at"),
        @Index(name = "idx_free_claim_listing_status", columnList = "listing_id,status")
})
public class FreeClaimRequest {

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
    private FreeClaimStatus status = FreeClaimStatus.PENDING;

    @Column(name = "owner_viewed_at")
    private Instant ownerViewedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_id")
    private Account cancelledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FreeClaimRequest() { }

    /** Creates an unread pending claim for an available Free listing. */
    public FreeClaimRequest(BookListing listing, Account requester, Account owner, Instant now) {
        this.listing = listing;
        this.requester = requester;
        this.owner = owner;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Records the owner's decision after the service reserves the listing. */
    public void accept(Instant now) { requirePending(); status = FreeClaimStatus.ACCEPTED; acceptedAt = now; }

    /** Declines a pending claim without changing listing availability. */
    public void decline() { requirePending(); status = FreeClaimStatus.DECLINED; }

    /** Closes another pending claim after the owner accepts a competing claim. */
    public void close() { requirePending(); status = FreeClaimStatus.CLOSED; }

    /** Cancels a pending or accepted claim and records which participant cancelled it. */
    public void cancel(Account member, Instant now) {
        if (status != FreeClaimStatus.PENDING && status != FreeClaimStatus.ACCEPTED) {
            throw new IllegalArgumentException("This Free-book claim can no longer be cancelled.");
        }
        status = FreeClaimStatus.CANCELLED;
        cancelledBy = member;
        cancelledAt = now;
    }

    /** Marks an incoming pending claim reviewed for the owner's unread badge. */
    public void markOwnerViewed(Instant now) { if (ownerViewedAt == null) ownerViewedAt = now; }

    private void requirePending() {
        if (status != FreeClaimStatus.PENDING) {
            throw new IllegalArgumentException("This Free-book claim is no longer pending.");
        }
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public BookListing getListing() { return listing; }
    public Account getRequester() { return requester; }
    public Account getOwner() { return owner; }
    public FreeClaimStatus getStatus() { return status; }
    public Instant getOwnerViewedAt() { return ownerViewedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Account getCancelledBy() { return cancelledBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
