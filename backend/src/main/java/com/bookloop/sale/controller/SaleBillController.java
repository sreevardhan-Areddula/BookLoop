package com.bookloop.sale.controller;



import com.bookloop.sale.dto.SaleBillCheckResponse;
import com.bookloop.sale.dto.SaleBillUploadResponse;
import com.bookloop.sale.service.SaleBillDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/sale-bills")
public class SaleBillController {

    private final SaleBillDocumentService billService;

    public SaleBillController(SaleBillDocumentService billService) {
        this.billService = billService;
    }

    /** Stores one authenticated member's private JPEG, PNG, or PDF bill and returns its opaque ID. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SaleBillUploadResponse> upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {
        SaleBillUploadResponse response = billService.uploadBill(UUID.fromString(jwt.getSubject()), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Checks the asking price against the paid amount read from a bill owned by the caller. */
    @PostMapping(value = "/{id}/check")
    public SaleBillCheckResponse check(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam BigDecimal askingPrice
    ) {
        return billService.checkBill(UUID.fromString(jwt.getSubject()), id, askingPrice);
    }
}
