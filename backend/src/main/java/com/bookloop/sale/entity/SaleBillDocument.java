package com.bookloop.sale.entity;



import com.bookloop.account.entity.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Stores a member's bill bytes in a private database row; no public file URL is created. */
@Entity
@Table(name = "sale_bill_documents")
public class SaleBillDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] fileData;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", nullable = false, length = 32)
    private SaleBillCheckStatus checkStatus = SaleBillCheckStatus.UPLOADED;

    @Column(name = "detected_paid_amount", precision = 10, scale = 2)
    private BigDecimal detectedPaidAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "checked_at")
    private Instant checkedAt;

    protected SaleBillDocument() {
    }

    public SaleBillDocument(Account owner, String originalFilename, String contentType, byte[] fileData) {
        this.owner = owner;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileData.length;
        this.fileData = fileData.clone();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Account getOwner() { return owner; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public byte[] getFileData() { return fileData.clone(); }
    public SaleBillCheckStatus getCheckStatus() { return checkStatus; }
    public BigDecimal getDetectedPaidAmount() { return detectedPaidAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCheckedAt() { return checkedAt; }

    /** Persists only the extracted paid amount and comparison outcome, never the raw OCR text. */
    public void recordCheck(SaleBillCheckStatus status, BigDecimal paidAmount, Instant checkedAt) {
        this.checkStatus = status;
        this.detectedPaidAmount = paidAmount;
        this.checkedAt = checkedAt;
    }
}
