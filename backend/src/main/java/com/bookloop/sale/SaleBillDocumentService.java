package com.bookloop.sale;

import com.bookloop.account.Account;
import com.bookloop.account.AccountRepository;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Owns private bill upload, storage, OCR invocation, and price-check persistence. */
@Service
public class SaleBillDocumentService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Pattern LABELED_AMOUNT = Pattern.compile(
            "(?im)(?:grand\\s*total|total\\s*paid|amount\\s*paid|paid\\s*amount|"
                    + "net\\s*(?:amount|payable)|order\\s*total|invoice\\s*total|"
                    + "total\\s*(?:amount|payable|due|paid)|(?<!sub)\\btotal\\b)"
                    + "[^\\r\\n\\d]{0,32}(?:₹|rs\\.?|inr)?\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)"
    );
    private final AccountRepository accountRepository;
    private final SaleBillDocumentRepository billRepository;
    private final SaleBillOcrService ocrService;
    private final int maxPdfPages;

    public SaleBillDocumentService(AccountRepository accountRepository,
                                   SaleBillDocumentRepository billRepository,
                                   SaleBillOcrService ocrService,
                                   @Value("${bookloop.sale-bills.max-pdf-pages:10}") int maxPdfPages) {
        this.accountRepository = accountRepository;
        this.billRepository = billRepository;
        this.ocrService = ocrService;
        this.maxPdfPages = maxPdfPages;
    }

    /** Validates the authenticated member's file and stores it as a private MySQL BLOB. */
    @Transactional
    public SaleBillUploadResponse uploadBill(UUID ownerId, MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("Choose a bill file to upload.");
        }
        if (upload.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Bill files must be 5 MB or smaller.");
        }
        Account owner = accountRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!owner.isEmailVerified() || owner.getCity() == null || owner.getCity().isBlank()) {
            throw new IllegalArgumentException("Verify your email and complete your city before uploading a sale bill.");
        }

        try {
            byte[] bytes = upload.getBytes();
            String detectedType = detectAndValidateFileType(bytes, upload.getContentType());
            if ("application/pdf".equals(detectedType)) {
                validatePdfPageCount(bytes);
            }
            String filename = safeFilename(upload.getOriginalFilename(), detectedType);
            SaleBillDocument saved = billRepository.save(new SaleBillDocument(owner, filename, detectedType, bytes));
            return SaleBillUploadResponse.from(saved);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The uploaded bill is not a readable JPEG, PNG, or PDF file.");
        }
    }

    /** Runs OCR for an owned bill and records whether the asking price is strictly below the paid amount. */
    @Transactional
    public SaleBillCheckResponse checkBill(UUID ownerId, UUID billId, BigDecimal askingPrice) {
        BigDecimal normalizedPrice = validateAskingPrice(askingPrice);
        SaleBillDocument bill = billRepository.findByIdAndOwner_Id(billId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Bill was not found."));
        String text = ocrService.recognizeBillText(bill.getFileData(), bill.getContentType());
        BigDecimal paidAmount = extractPaidAmount(text);
        Instant checkedAt = Instant.now();
        if (paidAmount == null) {
            bill.recordCheck(SaleBillCheckStatus.UNREADABLE, null, checkedAt);
            return SaleBillCheckResponse.from(bill, normalizedPrice, false,
                    "OCR could not identify one clear paid total. Upload a clearer bill to request the OCR price check.");
        }
        boolean passed = normalizedPrice.compareTo(paidAmount) < 0;
        SaleBillCheckStatus status = passed
                ? SaleBillCheckStatus.PRICE_CHECK_PASSED
                : SaleBillCheckStatus.PRICE_NOT_BELOW_PAID_AMOUNT;
        bill.recordCheck(status, paidAmount, checkedAt);
        String detail = passed
                ? "The asking price is below the paid amount read from the bill. This check does not authenticate the bill."
                : "The asking price must be lower than the paid amount read from the bill.";
        return SaleBillCheckResponse.from(bill, normalizedPrice, passed, detail);
    }

    /** Rejects unsupported content types and checks the actual file signature instead of trusting its name. */
    private String detectAndValidateFileType(byte[] bytes, String declaredContentType) {
        String detected;
        if (startsWith(bytes, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            detected = "image/jpeg";
        } else if (startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            detected = "image/png";
        } else if (bytes.length >= 5 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            detected = "application/pdf";
        } else {
            throw new IllegalArgumentException("Only JPEG, PNG, and PDF bill files are supported.");
        }
        String declared = declaredContentType == null ? "" : declaredContentType.toLowerCase(Locale.ROOT);
        if (!declared.isBlank() && !"application/octet-stream".equals(declared) && !detected.equals(declared)) {
            throw new IllegalArgumentException("The bill's file type does not match its contents.");
        }
        return detected;
    }

    /** Ensures an uploaded PDF parses successfully and contains no more than the approved page limit. */
    private void validatePdfPageCount(byte[] bytes) throws IOException {
        try (var document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() == 0) {
                throw new IllegalArgumentException("The PDF bill must contain at least one page.");
            }
            if (document.getNumberOfPages() > maxPdfPages) {
                throw new IllegalArgumentException("PDF bills may contain at most " + maxPdfPages + " pages.");
            }
        }
    }

    /** Removes path components and limits stored display filenames to a safe basename. */
    private String safeFilename(String original, String contentType) {
        String value = original == null ? "" : original.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        if (slash >= 0) value = value.substring(slash + 1);
        value = value.replaceAll("[\\p{Cntrl}]", "").trim();
        if (value.isBlank()) {
            value = "purchase-bill" + switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                default -> ".pdf";
            };
        }
        return value.length() <= 255 ? value : value.substring(value.length() - 255);
    }

    /** Accepts positive INR prices with no more than two decimal places and within the listing column range. */
    private BigDecimal validateAskingPrice(BigDecimal askingPrice) {
        if (askingPrice == null || askingPrice.signum() <= 0
                || askingPrice.compareTo(new BigDecimal("99999999.99")) > 0
                || askingPrice.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Asking price must be positive and use at most two decimal places.");
        }
        return askingPrice.setScale(2);
    }

    /** Finds exactly one labeled paid total; unlabeled amounts are never guessed to be the amount paid. */
    private BigDecimal extractPaidAmount(String recognizedText) {
        Set<BigDecimal> labeled = findAmounts(LABELED_AMOUNT, recognizedText);
        return labeled.size() == 1 ? labeled.iterator().next() : null;
    }

    /** Collects normalized decimal amounts from all matches so repeated totals remain one candidate. */
    private Set<BigDecimal> findAmounts(Pattern pattern, String text) {
        Set<BigDecimal> amounts = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        while (matcher.find()) {
            String numeric = matcher.group(1).replace(",", "");
            try {
                BigDecimal amount = new BigDecimal(numeric).setScale(2);
                if (amount.signum() > 0) amounts.add(amount);
            } catch (NumberFormatException ignored) {
                // Ignore OCR artifacts that resemble but do not parse as a monetary amount.
            }
        }
        return amounts;
    }

    /** Compares a file header against the known signature bytes. */
    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) return false;
        return Arrays.equals(Arrays.copyOf(bytes, signature.length), signature);
    }
}
