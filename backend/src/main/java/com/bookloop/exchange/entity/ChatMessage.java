package com.bookloop.exchange.entity;



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

@Entity
@Table(name = "exchange_messages", indexes = {
        @Index(name = "idx_exchange_message_poll", columnList = "exchange_id,created_at"),
        @Index(name = "idx_exchange_message_unread", columnList = "recipient_id,read_at")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_id", nullable = false)
    private ExchangeRequest exchange;

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

    protected ChatMessage() {
    }

    /** Creates a text message for the other participant in one exchange. */
    public ChatMessage(ExchangeRequest exchange, Account sender, Account recipient, String text, Instant createdAt) {
        this.exchange = exchange;
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
        this.createdAt = createdAt;
    }

    /** Marks a delivered message as read by its intended recipient. */
    public void markRead(Instant time) {
        if (readAt == null) readAt = time;
    }

    public UUID getId() { return id; }
    public ExchangeRequest getExchange() { return exchange; }
    public Account getSender() { return sender; }
    public Account getRecipient() { return recipient; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
