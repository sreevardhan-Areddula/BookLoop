package com.bookloop.book.entity;



import com.bookloop.account.entity.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A private text message shared by the owner and claimant after a Free claim is accepted. */
@Entity
@Table(name = "free_claim_messages", indexes = {
        @Index(name = "idx_free_claim_message_history", columnList = "claim_request_id,created_at"),
        @Index(name = "idx_free_claim_message_unread", columnList = "recipient_id,read_at")
})
public class FreeClaimMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_request_id", nullable = false)
    private FreeClaimRequest claimRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Account sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Account recipient;

    @Column(name = "message_text", nullable = false, length = 2000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected FreeClaimMessage() { }

    public FreeClaimMessage(FreeClaimRequest claimRequest, Account sender, Account recipient,
                            String text, Instant createdAt) {
        this.claimRequest = claimRequest;
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
        this.createdAt = createdAt;
    }

    /** Marks an unread message as read by its intended participant. */
    public void markRead(Instant now) { if (readAt == null) readAt = now; }

    public UUID getId() { return id; }
    public FreeClaimRequest getClaimRequest() { return claimRequest; }
    public Account getSender() { return sender; }
    public Account getRecipient() { return recipient; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
