package com.bookloop.sale;

import com.bookloop.account.Account;
import com.bookloop.account.AccountRepository;
import com.bookloop.book.BookListing;
import com.bookloop.book.BookListingRepository;
import com.bookloop.book.BookOfferType;
import com.bookloop.book.ListingAvailability;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SaleInterestRequestService {

    private final SaleInterestRequestRepository requestRepository;
    private final BookListingRepository listingRepository;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SaleInterestRequestService(SaleInterestRequestRepository requestRepository,
                                      BookListingRepository listingRepository,
                                      AccountRepository accountRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.requestRepository = requestRepository;
        this.listingRepository = listingRepository;
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Creates one member's pending interest in another member's available Sale listing. */
    @Transactional
    public SaleInterestRequestResponse createInterest(UUID requesterId, UUID listingId) {
        if (listingId == null) throw new IllegalArgumentException("Choose a sale listing.");
        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!requester.isEmailVerified() || requester.getCity() == null || requester.getCity().isBlank()) {
            throw new IllegalArgumentException("Complete your verified member profile before expressing interest.");
        }
        BookListing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Sale listing was not found."));
        if (listing.getOwner().getId().equals(requesterId)) {
            throw new IllegalArgumentException("You cannot express interest in your own listing.");
        }
        if (listing.getOfferType() != BookOfferType.SALE
                || listing.getAvailability() != ListingAvailability.AVAILABLE) {
            throw new IllegalArgumentException("This sale listing is not available for new interest requests.");
        }
        if (requestRepository.existsByRequester_IdAndListing_IdAndStatusIn(requesterId, listingId,
                List.of(SaleInterestStatus.PENDING, SaleInterestStatus.ACCEPTED))) {
            throw new IllegalArgumentException("You already have an active interest request for this book.");
        }
        Instant now = Instant.now();
        SaleInterestRequest saved = requestRepository.save(new SaleInterestRequest(
                listing, requester, listing.getOwner(), now));
        eventPublisher.publishEvent(new NewSaleInterestEvent(listing.getOwner().getEmail(),
                requester.getDisplayName(), listing.getTitle()));
        return SaleInterestRequestResponse.from(saved, requesterId);
    }

    /** Returns only requests where the member is the seller or interested buyer. */
    @Transactional(readOnly = true)
    public List<SaleInterestRequestResponse> listForMember(UUID memberId) {
        accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        return requestRepository.findAllByRequester_IdOrOwner_IdOrderByUpdatedAtDesc(memberId, memberId)
                .stream().map(request -> SaleInterestRequestResponse.from(request, memberId)).toList();
    }

    /** Marks one incoming pending request as seen by its seller for the in-app badge. */
    @Transactional
    public void markViewed(UUID memberId, UUID requestId) {
        SaleInterestRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Sale interest request was not found."));
        requireOwner(request, memberId);
        if (request.getStatus() == SaleInterestStatus.PENDING) request.markOwnerViewed(Instant.now());
    }

    /** Applies the seller's acceptance and optional reservation, closing other pending requests if reserved. */
    @Transactional
    public SaleInterestRequestResponse acceptInterest(UUID memberId, UUID requestId, boolean reserveListing) {
        SaleInterestRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Sale interest request was not found."));
        requireOwner(request, memberId);
        BookListing listing = listingRepository.findByIdForUpdate(request.getListing().getId())
                .orElseThrow(() -> new IllegalArgumentException("Sale listing was not found."));
        if (request.getStatus() != SaleInterestStatus.PENDING
                || listing.getAvailability() != ListingAvailability.AVAILABLE) {
            throw new IllegalArgumentException("This sale interest is no longer available to accept.");
        }
        Instant now = Instant.now();
        request.accept(reserveListing, now);
        if (reserveListing) {
            listing.setAvailability(ListingAvailability.RESERVED);
            requestRepository.findAllByListing_IdAndStatusAndIdNot(
                    listing.getId(), SaleInterestStatus.PENDING, requestId)
                    .forEach(SaleInterestRequest::close);
        }
        return SaleInterestRequestResponse.from(request, memberId);
    }

    /** Declines an incoming pending interest without changing listing availability. */
    @Transactional
    public SaleInterestRequestResponse declineInterest(UUID memberId, UUID requestId) {
        SaleInterestRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Sale interest request was not found."));
        requireOwner(request, memberId);
        request.decline();
        return SaleInterestRequestResponse.from(request, memberId);
    }

    /** Counts incoming pending requests that the seller has not opened yet. */
    @Transactional(readOnly = true)
    public SaleInterestUnreadCountResponse unreadCount(UUID memberId) {
        return new SaleInterestUnreadCountResponse(requestRepository.countByOwner_IdAndOwnerViewedAtIsNullAndStatus(
                memberId, SaleInterestStatus.PENDING));
    }

    private void requireOwner(SaleInterestRequest request, UUID memberId) {
        if (!request.getOwner().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only this listing's owner can review the interest request.");
        }
    }
}
