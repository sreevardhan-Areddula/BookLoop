package com.bookloop.sale.dto;



import com.bookloop.sale.entity.SaleBillCheckStatus;
import com.bookloop.sale.entity.SaleBillDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Bill price-check result; it deliberately exposes no bill bytes or raw OCR text. */
public record SaleBillCheckResponse(UUID billId, SaleBillCheckStatus status,
                                    BigDecimal askingPrice, BigDecimal detectedPaidAmount,
                                    boolean priceCheckPassed, String detail, Instant checkedAt) {
    public static SaleBillCheckResponse from(SaleBillDocument document, BigDecimal askingPrice,
                                             boolean passed, String detail) {
        return new SaleBillCheckResponse(document.getId(), document.getCheckStatus(), askingPrice,
                document.getDetectedPaidAmount(), passed, detail, document.getCheckedAt());
    }
}
