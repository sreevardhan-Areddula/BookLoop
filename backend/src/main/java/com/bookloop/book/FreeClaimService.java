package com.bookloop.book;

import com.bookloop.account.Account;
import com.bookloop.account.AccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Coordinates claims, listing reservations, notifications, and private Free-book conversations. */
@Service
public class FreeClaimService {

    private final FreeClaimRequestRepository claimRepository;
    private final FreeClaimMessageRepository messageRepository;
    private final BookListingRepository listingRepository;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FreeClaimService(FreeClaimRequestRepository claimRepository,
                            FreeClaimMessageRepository messageRepository,
                            BookListingRepository listingRepository,
                            AccountRepository accountRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.claimRepository = claimRepository;
        this.messageRepository = messageRepository;
        this.listingRepository = listingRepository;
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Creates a member-only pending claim for another member's available Free listing. */
    @Transactional
    public FreeClaimRequestResponse createClaim(UUID requesterId, UUID listingId) {
        if (listingId == null) throw new IllegalArgumentException("Choose a Free listing.");
        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!requester.isEmailVerified() || requester.getCity() == null || requester.getCity().isBlank()) {
            throw new IllegalArgumentException("Complete your verified member profile before requesting a Free book.");
        }
        BookListing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Free listing was not found."));
        if (listing.getOwner().getId().equals(requesterId)) {
            throw new IllegalArgumentException("You cannot claim your own listing.");
        }
        requireFreeAvailable(listing);
        if (claimRepository.existsByRequester_IdAndListing_IdAndStatusIn(requesterId, listingId,
                List.of(FreeClaimStatus.PENDING, FreeClaimStatus.ACCEPTED))) {
            throw new IllegalArgumentException("You already have an active claim for this book.");
        }
        Instant now = Instant.now();
        FreeClaimRequest saved = claimRepository.save(new FreeClaimRequest(listing, requester, listing.getOwner(), now));
        eventPublisher.publishEvent(new NewFreeClaimEvent(
                listing.getOwner().getEmail(), requester.getDisplayName(), listing.getTitle()));
        return FreeClaimRequestResponse.from(saved, requesterId);
    }

    /** Lists only claims involving the authenticated member. */
    @Transactional(readOnly = true)
    public List<FreeClaimRequestResponse> listForMember(UUID memberId) {
        accountRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        return claimRepository.findAllByRequester_IdOrOwner_IdOrderByUpdatedAtDesc(memberId, memberId)
                .stream().map(claim -> FreeClaimRequestResponse.from(claim, memberId)).toList();
    }

    /** Marks one incoming pending claim reviewed so it no longer increments the owner's badge. */
    @Transactional
    public void markViewed(UUID memberId, UUID claimId) {
        FreeClaimRequest initial = findClaim(claimId);
        listingRepository.findByIdForUpdate(initial.getListing().getId())
                .orElseThrow(() -> new IllegalArgumentException("Free listing was not found."));
        FreeClaimRequest claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Free-book claim was not found."));
        requireOwner(claim, memberId);
        if (claim.getStatus() == FreeClaimStatus.PENDING) claim.markOwnerViewed(Instant.now());
    }

    /** Accepts an owner-approved claim, reserves the book, and closes competing pending claims. */
    @Transactional
    public FreeClaimRequestResponse acceptClaim(UUID memberId, UUID claimId) {
        FreeClaimRequest initial = findClaim(claimId);
        BookListing listing = listingRepository.findByIdForUpdate(initial.getListing().getId())
                .orElseThrow(() -> new IllegalArgumentException("Free listing was not found."));
        FreeClaimRequest claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Free-book claim was not found."));
        requireOwner(claim, memberId);
        if (claim.getStatus() != FreeClaimStatus.PENDING) {
            throw new IllegalArgumentException("This Free-book claim is no longer pending.");
        }
        requireFreeAvailable(listing);
        Instant now = Instant.now();
        claim.accept(now);
        listing.setAvailability(ListingAvailability.RESERVED);
        claimRepository.findAllByListing_IdAndStatusAndIdNot(listing.getId(), FreeClaimStatus.PENDING, claimId)
                .forEach(FreeClaimRequest::close);
        return FreeClaimRequestResponse.from(claim, memberId);
    }

    /** Declines an incoming pending claim without changing the book's availability. */
    @Transactional
    public FreeClaimRequestResponse declineClaim(UUID memberId, UUID claimId) {
        FreeClaimRequest initial = findClaim(claimId);
        listingRepository.findByIdForUpdate(initial.getListing().getId())
                .orElseThrow(() -> new IllegalArgumentException("Free listing was not found."));
        FreeClaimRequest claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Free-book claim was not found."));
        requireOwner(claim, memberId);
        claim.decline();
        return FreeClaimRequestResponse.from(claim, memberId);
    }

    /** Cancels a participant's pending or accepted claim; an accepted cancellation releases the reservation. */
    @Transactional
    public FreeClaimRequestResponse cancelClaim(UUID memberId, UUID claimId) {
        FreeClaimRequest initial = findClaim(claimId);
        BookListing listing = listingRepository.findByIdForUpdate(initial.getListing().getId())
                .orElseThrow(() -> new IllegalArgumentException("Free listing was not found."));
        FreeClaimRequest claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Free-book claim was not found."));
        requireParticipant(claim, memberId);
        if (claim.getStatus() == FreeClaimStatus.ACCEPTED) {
            if (listing.getAvailability() != ListingAvailability.RESERVED) {
                throw new IllegalArgumentException("This Free listing is no longer reserved by this claim.");
            }
            listing.setAvailability(ListingAvailability.AVAILABLE);
        }
        Account member = accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        claim.cancel(member, Instant.now());
        return FreeClaimRequestResponse.from(claim, memberId);
    }

    /** Counts unreviewed incoming claims and unread messages for the Exchanges badge. */
    @Transactional(readOnly = true)
    public FreeClaimUnreadCountResponse unreadCount(UUID memberId) {
        long unreviewed = claimRepository.countByOwner_IdAndOwnerViewedAtIsNullAndStatus(memberId, FreeClaimStatus.PENDING);
        return new FreeClaimUnreadCountResponse(unreviewed + messageRepository.countByRecipient_IdAndReadAtIsNull(memberId));
    }

    /** Returns accepted or cancelled claim history to a participant and marks incoming messages read. */
    @Transactional
    public List<FreeClaimMessageResponse> listMessages(UUID memberId, UUID claimId, Instant after) {
        FreeClaimRequest claim = requireConversationParticipant(memberId, claimId);
        messageRepository.markConversationRead(claim.getId(), memberId, Instant.now());
        List<FreeClaimMessage> messages = after == null
                ? messageRepository.findAllByClaimRequest_IdOrderByCreatedAtAsc(claimId)
                : messageRepository.findAllByClaimRequest_IdAndCreatedAtAfterOrderByCreatedAtAsc(claimId, after);
        return messages.stream().map(FreeClaimMessageResponse::from).toList();
    }

    /** Sends a text message only while the owner-approved Free claim is active. */
    @Transactional
    public FreeClaimMessageResponse sendMessage(UUID memberId, UUID claimId, String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank() || normalized.length() > 2000) {
            throw new IllegalArgumentException("Message is required and must be 2000 characters or fewer.");
        }
        FreeClaimRequest claim = requireConversationParticipant(memberId, claimId);
        if (claim.getStatus() != FreeClaimStatus.ACCEPTED) {
            throw new IllegalArgumentException("Messages can be sent only during an accepted Free-book claim.");
        }
        Account sender = accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        Account recipient = claim.getOwner().getId().equals(memberId) ? claim.getRequester() : claim.getOwner();
        return FreeClaimMessageResponse.from(messageRepository.save(new FreeClaimMessage(
                claim, sender, recipient, normalized, Instant.now())));
    }

    private FreeClaimRequest requireConversationParticipant(UUID memberId, UUID claimId) {
        FreeClaimRequest claim = findClaim(claimId);
        requireParticipant(claim, memberId);
        if (claim.getStatus() != FreeClaimStatus.ACCEPTED && claim.getStatus() != FreeClaimStatus.CANCELLED) {
            throw new IllegalArgumentException("This Free-book claim has no chat history.");
        }
        return claim;
    }

    private FreeClaimRequest findClaim(UUID claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Free-book claim was not found."));
    }

    private void requireFreeAvailable(BookListing listing) {
        if (listing.getOfferType() != BookOfferType.FREE || listing.getAvailability() != ListingAvailability.AVAILABLE) {
            throw new IllegalArgumentException("This Free listing is no longer available for claims.");
        }
    }

    private void requireOwner(FreeClaimRequest claim, UUID memberId) {
        if (!claim.getOwner().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only the listing owner can accept or decline this claim.");
        }
    }

    private void requireParticipant(FreeClaimRequest claim, UUID memberId) {
        if (!claim.getOwner().getId().equals(memberId) && !claim.getRequester().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only the claimant and listing owner can access this request.");
        }
    }
}
