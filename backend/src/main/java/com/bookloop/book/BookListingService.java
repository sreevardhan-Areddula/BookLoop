package com.bookloop.book;

import com.bookloop.account.Account;
import com.bookloop.account.AccountRepository;
import com.bookloop.sale.SaleBillCheckStatus;
import com.bookloop.sale.SaleBillDocument;
import com.bookloop.sale.SaleBillDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class BookListingService {

    private static final List<String> ALLOWED_GENRES = List.of(
            "Biography", "Children", "Fantasy", "Fiction", "History", "Mystery",
            "Non-fiction", "Poetry", "Romance", "Science", "Science fiction", "Self-help", "Thriller"
    );

    private final BookListingRepository listingRepository;
    private final AccountRepository accountRepository;
    private final SaleBillDocumentRepository billRepository;

    public BookListingService(BookListingRepository listingRepository, AccountRepository accountRepository,
                              SaleBillDocumentRepository billRepository) {
        this.listingRepository = listingRepository;
        this.accountRepository = accountRepository;
        this.billRepository = billRepository;
    }

    /** Validates a member's book details, derives its public city from their profile, and stores it. */
    @Transactional
    public BookListingResponse create(UUID ownerId, BookListingRequest request) {
        String title = requiredText(request.title(), "Title", 255);
        String author = requiredText(request.author(), "Author", 255);
        if (request.condition() == null || request.offerType() == null) {
            throw new IllegalArgumentException("Choose a book condition and offer type.");
        }
        if (request.genres() == null || request.genres().isEmpty()) {
            throw new IllegalArgumentException("Choose at least one genre.");
        }
        List<String> genres = request.genres().stream()
                .map(genre -> ALLOWED_GENRES.stream().filter(allowed -> allowed.equalsIgnoreCase(genre.trim()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Choose genres from the available list.")))
                .distinct().toList();

        BigDecimal askingPrice = request.askingPrice();
        if (request.offerType() == BookOfferType.SALE) {
            askingPrice = validateSalePrice(askingPrice);
        } else if (askingPrice != null || request.saleBillDocumentId() != null) {
            throw new IllegalArgumentException("A price is only used for sale listings.");
        }
        Account owner = accountRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!owner.isEmailVerified() || owner.getCity() == null || owner.getCity().isBlank()) {
            throw new IllegalArgumentException("Verify your email and complete your city before posting books.");
        }

        String isbn = optionalText(request.isbn(), 20, "ISBN");
        String description = request.description() == null ? null : request.description().trim();
        if (description != null && description.length() > 4000) {
            throw new IllegalArgumentException("Description must be 4000 characters or fewer.");
        }
        BookListing listing = new BookListing(
                owner, title, author, genres, request.condition(), owner.getCity(), request.offerType(),
                askingPrice, isbn, description == null || description.isBlank() ? null : description
        );
        if (request.offerType() == BookOfferType.SALE && request.saleBillDocumentId() != null) {
            SaleBillDocument bill = getPassedBill(ownerId, request.saleBillDocumentId(), askingPrice);
            listing.markSalePriceVerified(bill.getId());
        }
        BookListing saved = listingRepository.save(listing);
        return BookListingResponse.from(saved);
    }

    /** Links an owner's bill to their existing sale listing after its OCR amount passes the current price. */
    @Transactional
    public BookListingResponse attachSaleVerification(UUID ownerId, UUID listingId, UUID billId) {
        BookListing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Sale listing was not found."));
        if (!listing.getOwner().getId().equals(ownerId)) {
            throw new IllegalArgumentException("Sale listing was not found.");
        }
        if (listing.getOfferType() != BookOfferType.SALE) {
            throw new IllegalArgumentException("Bill verification is only available for sale listings.");
        }
        SaleBillDocument bill = getPassedBill(ownerId, billId, listing.getAskingPrice());
        listing.markSalePriceVerified(bill.getId());
        return BookListingResponse.from(listing);
    }

    private SaleBillDocument getPassedBill(UUID ownerId, UUID billId, BigDecimal askingPrice) {
        SaleBillDocument bill = billRepository.findByIdAndOwner_Id(billId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Checked bill was not found."));
        if (bill.getCheckStatus() != SaleBillCheckStatus.PRICE_CHECK_PASSED
                || bill.getDetectedPaidAmount() == null
                || askingPrice.compareTo(bill.getDetectedPaidAmount()) >= 0) {
            throw new IllegalArgumentException("The bill must pass OCR with a paid amount above this sale price.");
        }
        return bill;
    }

    private BigDecimal validateSalePrice(BigDecimal askingPrice) {
        if (askingPrice == null || askingPrice.signum() <= 0
                || askingPrice.compareTo(new BigDecimal("99999999.99")) > 0
                || askingPrice.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Sale price must be positive and use at most two decimal places.");
        }
        return askingPrice.setScale(2);
    }

    /** Finds newest listings by partial title/city and exact selected genre. */
    @Transactional(readOnly = true)
    public List<BookListingResponse> search(String title, String genre, String city, BookOfferType offerType) {
        return listingRepository.search(cleanFilter(title), cleanFilter(genre), cleanFilter(city), offerType)
                .stream().map(BookListingResponse::from).toList();
    }

    /** Returns only the authenticated member's listings, newest first. */
    @Transactional(readOnly = true)
    public List<BookListingResponse> mine(UUID ownerId) {
        return listingRepository.findAllByOwner_IdOrderByCreatedAtDesc(ownerId)
                .stream().map(BookListingResponse::from).toList();
    }

    private String requiredText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is required and must be at most " + maxLength + " characters.");
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) return null;
        return requiredText(value, label, maxLength);
    }

    private String cleanFilter(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
