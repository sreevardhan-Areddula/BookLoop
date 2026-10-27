package com.bookloop.exchange;

import com.bookloop.account.Account;
import com.bookloop.book.BookListing;
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
@Table(name = "exchange_requests", indexes = {
        @Index(name = "idx_exchange_requester_status", columnList = "requester_id,status,created_at"),
        @Index(name = "idx_exchange_owner_status", columnList = "owner_id,status,created_at"),
        @Index(name = "idx_exchange_requested_status", columnList = "requested_listing_id,status"),
        @Index(name = "idx_exchange_offered_status", columnList = "offered_listing_id,status"),
        @Index(name = "idx_exchange_expiry", columnList = "status,expires_at")
})
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Account requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_listing_id", nullable = false)
    private BookListing requestedListing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offered_listing_id", nullable = false)
    private BookListing offeredListing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_id")
    private Account cancelledBy;

    @Column(name = "owner_viewed_at")
    private Instant ownerViewedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "requester_completed_at")
    private Instant requesterCompletedAt;

    @Column(name = "owner_completed_at")
    private Instant ownerCompletedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExchangeRequest() {
    }

    /** Creates a pending exchange proposal with its 7-day response deadline. */
    public ExchangeRequest(Account requester, Account owner, BookListing requestedListing,
                           BookListing offeredListing, Instant now, Instant expiresAt) {
        this.requester = requester;
        this.owner = owner;
        this.requestedListing = requestedListing;
        this.offeredListing = offeredListing;
        this.status = ExchangeStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
        this.expiresAt = expiresAt;
    }

    /** Accepts a pending proposal after the exchange service has reserved both listings. */
    public void accept(Instant now) {
        requireStatus(ExchangeStatus.PENDING);
        status = ExchangeStatus.ACCEPTED;
        acceptedAt = now;
    }

    /** Declines an unanswered proposal without changing either listing's availability. */
    public void decline() {
        requireStatus(ExchangeStatus.PENDING);
        status = ExchangeStatus.DECLINED;
    }

    /** Closes an unanswered proposal because one of its books was reserved elsewhere. */
    public void close() {
        requireStatus(ExchangeStatus.PENDING);
        status = ExchangeStatus.CLOSED;
    }

    /** Cancels an unanswered or accepted exchange and records the member who canceled it. */
    public void cancel(Account member, Instant now) {
        if (status != ExchangeStatus.PENDING && status != ExchangeStatus.ACCEPTED) {
            throw new IllegalArgumentException("This exchange can no longer be canceled.");
        }
        status = ExchangeStatus.CANCELLED;
        cancelledBy = member;
        cancelledAt = now;
    }

    /** Records one participant's completion confirmation and returns true when both confirmed. */
    public boolean confirmCompletion(UUID memberId, Instant now) {
        requireStatus(ExchangeStatus.ACCEPTED);
        if (requester.getId().equals(memberId)) {
            requesterCompletedAt = now;
        } else if (owner.getId().equals(memberId)) {
            ownerCompletedAt = now;
        } else {
            throw new IllegalArgumentException("Only exchange participants can confirm completion.");
        }
        if (requesterCompletedAt != null && ownerCompletedAt != null) {
            status = ExchangeStatus.COMPLETED;
            completedAt = now;
            return true;
        }
        return false;
    }

    /** Expires an unanswered request after its response deadline. */
    public void expire() {
        requireStatus(ExchangeStatus.PENDING);
        status = ExchangeStatus.EXPIRED;
    }

    /** Marks the incoming request as seen by its owner so it no longer increments the badge. */
    public void markOwnerViewed(Instant now) {
        if (ownerViewedAt == null) ownerViewedAt = now;
    }

    private void requireStatus(ExchangeStatus expected) {
        if (status != expected) {
            throw new IllegalArgumentException("This exchange is no longer " + expected.name().toLowerCase() + ".");
        }
    }

    public UUID getId() { return id; }
    public Account getRequester() { return requester; }
    public Account getOwner() { return owner; }
    public BookListing getRequestedListing() { return requestedListing; }
    public BookListing getOfferedListing() { return offeredListing; }
    public ExchangeStatus getStatus() { return status; }
    public Account getCancelledBy() { return cancelledBy; }
    public Instant getOwnerViewedAt() { return ownerViewedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getRequesterCompletedAt() { return requesterCompletedAt; }
    public Instant getOwnerCompletedAt() { return ownerCompletedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
