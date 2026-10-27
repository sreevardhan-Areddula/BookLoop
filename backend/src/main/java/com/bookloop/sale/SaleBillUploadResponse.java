package com.bookloop.sale;

import java.time.Instant;
import java.util.UUID;

/** Safe metadata returned after a private purchase bill is stored. */
public record SaleBillUploadResponse(UUID id, String filename, String contentType,
                                     long sizeBytes, SaleBillCheckStatus checkStatus,
                                     Instant uploadedAt) {
    public static SaleBillUploadResponse from(SaleBillDocument document) {
        return new SaleBillUploadResponse(document.getId(), document.getOriginalFilename(),
                document.getContentType(), document.getFileSize(), document.getCheckStatus(),
                document.getCreatedAt());
    }
}
