package com.bookloop.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "pending_email", unique = true, length = 320)
    private String pendingEmail;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    private String city;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {
        // Required by JPA.
    }

    Account(String displayName, String email, String passwordHash) {
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPendingEmail() {
        return pendingEmail;
    }

    /** Replaces the member's display name after profile-service validation. */
    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** Stores the requested address without changing the currently active sign-in email. */
    public void setPendingEmail(String pendingEmail) {
        this.pendingEmail = pendingEmail;
    }

    /** Promotes the verified pending address and clears its temporary state. */
    public void confirmPendingEmail() {
        if (pendingEmail == null) {
            throw new IllegalStateException("There is no pending email address to confirm.");
        }
        this.email = pendingEmail;
        this.pendingEmail = null;
        this.emailVerified = true;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    String passwordHash() {
        return passwordHash;
    }

    public void updateCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    /** Records the latest activity timestamp for private exchange presence. */
    public void recordActivity(Instant time) {
        this.lastActiveAt = time;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void prepareForInsert() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }

        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void prepareForUpdate() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }

        updatedAt = Instant.now();
    }
}
