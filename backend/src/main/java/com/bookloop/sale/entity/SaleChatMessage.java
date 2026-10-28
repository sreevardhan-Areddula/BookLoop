package com.bookloop.sale.entity;



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

/** A private text message or seller-shared bill event in an accepted Sale conversation. */
@Entity
@Table(name = "sale_chat_messages", indexes = {
        @Index(name = "idx_sale_chat_history", columnList = "interest_request_id,created_at"),
        @Index(name = "idx_sale_chat_unread", columnList = "recipient_id,read_at")
})
public class SaleChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_request_id", nullable = false)
    private SaleInterestRequest interestRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Account sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Account recipient;

    @Column(name = "message_text", length = 2000)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_bill_document_id")
    private SaleSharedBillDocument sharedBill;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected SaleChatMessage() { }

    public SaleChatMessage(SaleInterestRequest interestRequest, Account sender, Account recipient,
                           String text, SaleSharedBillDocument sharedBill, Instant createdAt) {
        this.interestRequest = interestRequest;
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
        this.sharedBill = sharedBill;
        this.createdAt = createdAt;
    }

    /** Marks an unread Sale chat message as read by its intended recipient. */
    public void markRead(Instant now) { if (readAt == null) readAt = now; }

    public UUID getId() { return id; }
    public SaleInterestRequest getInterestRequest() { return interestRequest; }
    public Account getSender() { return sender; }
    public Account getRecipient() { return recipient; }
    public String getText() { return text; }
    public SaleSharedBillDocument getSharedBill() { return sharedBill; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
