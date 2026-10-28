package com.bookloop.sale;

import com.bookloop.account.Account;
import com.bookloop.account.AccountRepository;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Controls private messages and seller-selected bill copies for accepted Sale interests. */
@Service
public class SaleChatService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private final SaleInterestRequestRepository interestRepository;
    private final SaleChatMessageRepository messageRepository;
    private final SaleSharedBillDocumentRepository billRepository;
    private final AccountRepository accountRepository;
    private final int maxPdfPages;

    public SaleChatService(SaleInterestRequestRepository interestRepository,
                           SaleChatMessageRepository messageRepository,
                           SaleSharedBillDocumentRepository billRepository,
                           AccountRepository accountRepository,
                           @Value("${bookloop.sale-bills.max-pdf-pages:10}") int maxPdfPages) {
        this.interestRepository = interestRepository;
        this.messageRepository = messageRepository;
        this.billRepository = billRepository;
        this.accountRepository = accountRepository;
        this.maxPdfPages = maxPdfPages;
    }

    /** Loads accepted-conversation history for a participant and marks their incoming messages read. */
    @Transactional
    public List<SaleChatMessageResponse> listSaleMessages(UUID memberId, UUID interestId, Instant after) {
        SaleInterestRequest interest = requireAcceptedParticipant(memberId, interestId);
        messageRepository.markConversationRead(interest.getId(), memberId, Instant.now());
        List<SaleChatMessage> messages = after == null
                ? messageRepository.findAllByInterestRequest_IdOrderByCreatedAtAsc(interestId)
                : messageRepository.findAllByInterestRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(interestId, after);
        return messages.stream().map(SaleChatMessageResponse::from).toList();
    }

    /** Sends up to 2,000 characters from one participant to the other in an accepted Sale interest. */
    @Transactional
    public SaleChatMessageResponse sendSaleMessage(UUID memberId, UUID interestId, String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank() || normalized.length() > 2000) {
            throw new IllegalArgumentException("Message is required and must be 2000 characters or fewer.");
        }
        SaleInterestRequest interest = requireAcceptedParticipant(memberId, interestId);
        Account sender = accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        Account recipient = otherParticipant(interest, memberId);
        SaleChatMessage saved = messageRepository.save(new SaleChatMessage(
                interest, sender, recipient, normalized, null, Instant.now()));
        return SaleChatMessageResponse.from(saved);
    }

    /** Stores a seller-selected redacted JPEG, PNG, or PDF and posts it privately to the accepted conversation. */
    @Transactional
    public SaleChatMessageResponse shareRedactedBill(UUID memberId, UUID interestId, MultipartFile upload) {
        SaleInterestRequest interest = requireAcceptedParticipant(memberId, interestId);
        if (!interest.getOwner().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only the seller can share a bill copy.");
        }
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("Choose the redacted bill copy to share.");
        }
        if (upload.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Shared bill copies must be 5 MB or smaller.");
        }
        try {
            byte[] bytes = upload.getBytes();
            String contentType = detectAndValidateType(bytes, upload.getContentType());
            if ("application/pdf".equals(contentType)) validatePdfPageCount(bytes);
            String filename = safeFilename(upload.getOriginalFilename(), contentType);
            Account seller = accountRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
            SaleSharedBillDocument bill = billRepository.save(new SaleSharedBillDocument(
                    interest, seller, filename, contentType, bytes));
            SaleChatMessage message = messageRepository.save(new SaleChatMessage(
                    interest, seller, interest.getRequester(), null, bill, Instant.now()));
            return SaleChatMessageResponse.from(message);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The shared copy is not a readable JPEG, PNG, or PDF file.");
        }
    }

    /** Returns a shared bill's bytes only when the caller participates in its accepted conversation. */
    @Transactional(readOnly = true)
    public SaleSharedBillFile downloadSharedBill(UUID memberId, UUID interestId, UUID messageId) {
        SaleInterestRequest interest = requireAcceptedParticipant(memberId, interestId);
        SaleChatMessage message = messageRepository.findByIdAndInterestRequest_Id(messageId, interestId)
                .orElseThrow(() -> new IllegalArgumentException("Shared bill was not found."));
        SaleSharedBillDocument bill = message.getSharedBill();
        if (bill == null) throw new IllegalArgumentException("This message has no shared bill.");
        return new SaleSharedBillFile(bill.getOriginalFilename(), bill.getContentType(), bill.getFileData());
    }

    private SaleInterestRequest requireAcceptedParticipant(UUID memberId, UUID interestId) {
        SaleInterestRequest interest = interestRepository.findById(interestId)
                .orElseThrow(() -> new IllegalArgumentException("Sale interest request was not found."));
        if (!interest.getOwner().getId().equals(memberId) && !interest.getRequester().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only the buyer and seller can access this conversation.");
        }
        if (interest.getStatus() != SaleInterestStatus.ACCEPTED) {
            throw new IllegalArgumentException("Sale chat is available only after the seller accepts the interest.");
        }
        return interest;
    }

    private Account otherParticipant(SaleInterestRequest interest, UUID memberId) {
        return interest.getOwner().getId().equals(memberId) ? interest.getRequester() : interest.getOwner();
    }

    private String detectAndValidateType(byte[] bytes, String declaredType) {
        String detected;
        if (startsWith(bytes, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) detected = "image/jpeg";
        else if (startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) detected = "image/png";
        else if (bytes.length >= 5 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) detected = "application/pdf";
        else throw new IllegalArgumentException("Only JPEG, PNG, and PDF bill copies are supported.");
        String declared = declaredType == null ? "" : declaredType.toLowerCase(Locale.ROOT);
        if (!declared.isBlank() && !"application/octet-stream".equals(declared) && !detected.equals(declared)) {
            throw new IllegalArgumentException("The shared copy's file type does not match its contents.");
        }
        return detected;
    }

    private void validatePdfPageCount(byte[] bytes) throws IOException {
        try (var document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() == 0 || document.getNumberOfPages() > maxPdfPages) {
                throw new IllegalArgumentException("PDF bill copies must contain 1 to " + maxPdfPages + " pages.");
            }
        }
    }

    private String safeFilename(String original, String contentType) {
        String filename = original == null ? "" : original.replace('\\', '/');
        int slash = filename.lastIndexOf('/');
        if (slash >= 0) filename = filename.substring(slash + 1);
        filename = filename.replaceAll("[\\p{Cntrl}]", "").trim();
        if (filename.isBlank()) filename = "redacted-bill" + switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".pdf";
        };
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length && Arrays.equals(Arrays.copyOf(bytes, signature.length), signature);
    }
}
