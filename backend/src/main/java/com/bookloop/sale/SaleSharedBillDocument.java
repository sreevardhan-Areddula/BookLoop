package com.bookloop.sale;

import com.bookloop.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Stores a seller-selected redacted bill copy privately for one Sale interest conversation. */
@Entity
@Table(name = "sale_shared_bill_documents")
public class SaleSharedBillDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_request_id", nullable = false)
    private SaleInterestRequest interestRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Account uploader;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] fileData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SaleSharedBillDocument() { }

    public SaleSharedBillDocument(SaleInterestRequest interestRequest, Account uploader,
                                  String originalFilename, String contentType, byte[] fileData) {
        this.interestRequest = interestRequest;
        this.uploader = uploader;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileData.length;
        this.fileData = fileData.clone();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public SaleInterestRequest getInterestRequest() { return interestRequest; }
    public Account getUploader() { return uploader; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public byte[] getFileData() { return fileData.clone(); }
    public Instant getCreatedAt() { return createdAt; }
}
