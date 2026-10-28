package com.bookloop.account.entity;


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
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_verification_account", columnList = "account_id")
})
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "verification_email", length = 320)
    private String verificationEmail;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(Account account, String tokenHash, Instant expiresAt) {
        this(account, tokenHash, expiresAt, null);
    }

    public EmailVerificationToken(Account account, String tokenHash, Instant expiresAt, String verificationEmail) {
        this.account = account;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.verificationEmail = verificationEmail;
        this.createdAt = Instant.now();
    }

    public Account getAccount() {
        return account;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getVerificationEmail() {
        return verificationEmail;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markUsed(Instant time) {
        this.usedAt = time;
    }
}
