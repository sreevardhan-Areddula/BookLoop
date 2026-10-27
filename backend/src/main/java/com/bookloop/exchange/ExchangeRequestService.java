package com.bookloop.exchange;

import com.bookloop.account.Account;
import com.bookloop.account.AccountRepository;
import com.bookloop.book.BookListing;
import com.bookloop.book.BookListingRepository;
import com.bookloop.book.BookOfferType;
import com.bookloop.book.ListingAvailability;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ExchangeRequestService {

    private final ExchangeRequestRepository exchangeRepository;
    private final BookListingRepository listingRepository;
    private final AccountRepository accountRepository;
    private final ChatMessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExchangeRequestService(ExchangeRequestRepository exchangeRepository,
                                  BookListingRepository listingRepository,
                                  AccountRepository accountRepository,
                                  ChatMessageRepository messageRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.exchangeRepository = exchangeRepository;
        this.listingRepository = listingRepository;
        this.accountRepository = accountRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Creates a 7-day pending request for two available Exchange listings owned by different members. */
    @Transactional
    public ExchangeRequestResponse createRequest(UUID requesterId, UUID requestedListingId, UUID offeredListingId) {
        if (requestedListingId == null || offeredListingId == null || requestedListingId.equals(offeredListingId)) {
            throw new IllegalArgumentException("Choose two different Exchange books.");
        }
        Account requester = accountRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!requester.isEmailVerified() || requester.getCity() == null || requester.getCity().isBlank()) {
            throw new IllegalArgumentException("Complete your verified member profile before requesting an exchange.");
        }

        var books = lockListingPair(requestedListingId, offeredListingId);
        BookListing requested = books.get(requestedListingId);
        BookListing offered = books.get(offeredListingId);
        if (requested.getOwner().getId().equals(requesterId)) {
            throw new IllegalArgumentException("You cannot request your own book.");
        }
        if (!offered.getOwner().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Choose an Exchange book from your own listings.");
        }
        requireExchangeAvailable(requested);
        requireExchangeAvailable(offered);
        if (exchangeRepository.existsByRequester_IdAndRequestedListing_IdAndOfferedListing_IdAndStatusIn(
                requesterId, requestedListingId, offeredListingId, List.of(ExchangeStatus.PENDING, ExchangeStatus.ACCEPTED))) {
            throw new IllegalArgumentException("You already have an active request for this book pair.");
        }

        Instant now = Instant.now();
        ExchangeRequest exchange = exchangeRepository.save(new ExchangeRequest(
                requester, requested.getOwner(), requested, offered, now, now.plus(7, ChronoUnit.DAYS)
        ));
        eventPublisher.publishEvent(new NewExchangeRequestEvent(requested.getOwner().getEmail(),
                requester.getDisplayName(), requested.getTitle(), offered.getTitle()));
        return ExchangeRequestResponse.from(exchange, requesterId);
    }

    /** Lists only exchanges involving the member without changing whether incoming requests were reviewed. */
    @Transactional
    public List<ExchangeRequestResponse> listForMember(UUID memberId) {
        accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        List<ExchangeRequest> exchanges = exchangeRepository
                .findAllByRequester_IdOrOwner_IdOrderByUpdatedAtDesc(memberId, memberId);
        return exchanges.stream().map(exchange -> ExchangeRequestResponse.from(exchange, memberId)).toList();
    }

    /** Marks one incoming request reviewed after its owner explicitly opens that request. */
    @Transactional
    public void markRequestViewed(UUID memberId, UUID exchangeId) {
        ExchangeRequest exchange = lockedExchange(exchangeId);
        requireOwner(exchange, memberId);
        if (exchange.getStatus() == ExchangeStatus.PENDING) {
            exchange.markOwnerViewed(Instant.now());
        }
    }

    /** Reserves both books and closes pending requests that reference either listing. */
    @Transactional
    public ExchangeRequestResponse acceptRequest(UUID memberId, UUID exchangeId) {
        ExchangeRequest snapshot = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request was not found."));
        var books = lockListingPair(snapshot.getRequestedListing().getId(), snapshot.getOfferedListing().getId());
        ExchangeRequest exchange = exchangeRepository.findByIdForUpdate(exchangeId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request was not found."));
        requireOwner(exchange, memberId);
        if (exchange.getStatus() != ExchangeStatus.PENDING || !exchange.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("This exchange request is no longer available to accept.");
        }
        BookListing requested = books.get(exchange.getRequestedListing().getId());
        BookListing offered = books.get(exchange.getOfferedListing().getId());
        requireExchangeAvailable(requested);
        requireExchangeAvailable(offered);

        Instant now = Instant.now();
        exchange.accept(now);
        requested.setAvailability(ListingAvailability.RESERVED);
        offered.setAvailability(ListingAvailability.RESERVED);
        List<UUID> listingIds = List.of(requested.getId(), offered.getId());
        exchangeRepository.findConflicts(ExchangeStatus.PENDING, listingIds).forEach(ExchangeRequest::close);
        return ExchangeRequestResponse.from(exchange, memberId);
    }

    /** Declines an incoming pending request without reserving its books. */
    @Transactional
    public ExchangeRequestResponse declineRequest(UUID memberId, UUID exchangeId) {
        ExchangeRequest exchange = lockedExchange(exchangeId);
        requireOwner(exchange, memberId);
        if (!exchange.getExpiresAt().isAfter(Instant.now())) {
            exchange.expire();
        } else {
            exchange.decline();
        }
        return ExchangeRequestResponse.from(exchange, memberId);
    }

    /** Cancels a request or accepted exchange; accepted books return to Browse and closed conflicts stay closed. */
    @Transactional
    public ExchangeRequestResponse cancelRequest(UUID memberId, UUID exchangeId) {
        ExchangeRequest exchange = lockedExchange(exchangeId);
        requireParticipant(exchange, memberId);
        if (exchange.getStatus() == ExchangeStatus.ACCEPTED) {
            var books = lockListingPair(exchange.getRequestedListing().getId(), exchange.getOfferedListing().getId());
            books.values().forEach(book -> book.setAvailability(ListingAvailability.AVAILABLE));
        }
        Account member = accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        exchange.cancel(member, Instant.now());
        return ExchangeRequestResponse.from(exchange, memberId);
    }

    /** Records the member's completion confirmation and removes both books after mutual confirmation. */
    @Transactional
    public ExchangeRequestResponse confirmCompletion(UUID memberId, UUID exchangeId) {
        ExchangeRequest exchange = lockedExchange(exchangeId);
        requireParticipant(exchange, memberId);
        boolean completed = exchange.confirmCompletion(memberId, Instant.now());
        if (completed) {
            var books = lockListingPair(exchange.getRequestedListing().getId(), exchange.getOfferedListing().getId());
            books.values().forEach(book -> book.setAvailability(ListingAvailability.EXCHANGED));
        }
        return ExchangeRequestResponse.from(exchange, memberId);
    }

    /** Expires unanswered requests every minute once their 7-day response window has elapsed. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingRequests() {
        exchangeRepository.findAllByStatusAndExpiresAtBefore(ExchangeStatus.PENDING, Instant.now())
                .forEach(ExchangeRequest::expire);
    }

    /** Saves a normalized text message only while the exchange is accepted and active. */
    @Transactional
    public ChatMessageResponse sendMessage(UUID memberId, UUID exchangeId, String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank() || normalized.length() > 2000) {
            throw new IllegalArgumentException("Message is required and must be 2000 characters or fewer.");
        }
        ExchangeRequest exchange = lockedExchange(exchangeId);
        requireParticipant(exchange, memberId);
        if (exchange.getStatus() != ExchangeStatus.ACCEPTED) {
            throw new IllegalArgumentException("Messages can only be sent during an active accepted exchange.");
        }
        Account sender = accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        Account recipient = exchange.getRequester().getId().equals(memberId)
                ? exchange.getOwner() : exchange.getRequester();
        ChatMessage saved = messageRepository.save(new ChatMessage(exchange, sender, recipient,
                normalized, Instant.now()));
        return ChatMessageResponse.from(saved);
    }

    /** Returns conversation history to participants and marks messages to the viewer as read. */
    @Transactional
    public List<ChatMessageResponse> listMessages(UUID memberId, UUID exchangeId, Instant after) {
        ExchangeRequest exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request was not found."));
        requireParticipant(exchange, memberId);
        if (exchange.getStatus() != ExchangeStatus.ACCEPTED
                && exchange.getStatus() != ExchangeStatus.CANCELLED
                && exchange.getStatus() != ExchangeStatus.COMPLETED) {
            throw new IllegalArgumentException("This exchange has no chat history.");
        }
        messageRepository.markConversationRead(exchangeId, memberId, Instant.now());
        List<ChatMessage> messages = after == null
                ? messageRepository.findAllByExchange_IdOrderByCreatedAtAsc(exchangeId)
                : messageRepository.findAllByExchange_IdAndCreatedAtAfterOrderByCreatedAtAsc(exchangeId, after);
        return messages.stream().map(ChatMessageResponse::from).toList();
    }

    /** Updates the viewer's activity and returns only their exchange partner's status. */
    @Transactional
    public PresenceResponse recordPresence(UUID memberId, UUID exchangeId) {
        ExchangeRequest exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request was not found."));
        requireParticipant(exchange, memberId);
        if (exchange.getStatus() != ExchangeStatus.ACCEPTED
                && exchange.getStatus() != ExchangeStatus.CANCELLED
                && exchange.getStatus() != ExchangeStatus.COMPLETED) {
            throw new IllegalArgumentException("Presence is available only in an exchange conversation.");
        }
        Account viewer = accountRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        Instant now = Instant.now();
        viewer.recordActivity(now);
        Account other = exchange.getRequester().getId().equals(memberId)
                ? exchange.getOwner() : exchange.getRequester();
        Instant lastActive = other.getLastActiveAt();
        boolean online = lastActive != null && lastActive.isAfter(now.minus(Duration.ofMinutes(2)));
        return new PresenceResponse(other.getDisplayName(), online, lastActive);
    }

    /** Combines unseen incoming requests and unread messages for the header badge. */
    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID memberId) {
        long incoming = exchangeRepository.countByOwner_IdAndOwnerViewedAtIsNullAndStatus(
                memberId, ExchangeStatus.PENDING);
        return new UnreadCountResponse(incoming + messageRepository.countByRecipient_IdAndReadAtIsNull(memberId));
    }

    private ExchangeRequest lockedExchange(UUID exchangeId) {
        return exchangeRepository.findByIdForUpdate(exchangeId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request was not found."));
    }

    /** Locks book rows by sorted UUID so two requests acquire shared listing locks in a stable order. */
    private java.util.Map<UUID, BookListing> lockListingPair(UUID firstId, UUID secondId) {
        List<UUID> ids = new ArrayList<>(List.of(firstId, secondId));
        ids.sort(Comparator.comparing(UUID::toString));
        java.util.Map<UUID, BookListing> books = new java.util.HashMap<>();
        for (UUID id : ids) {
            BookListing listing = listingRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Book listing was not found."));
            books.put(id, listing);
        }
        return books;
    }

    private void requireExchangeAvailable(BookListing listing) {
        if (listing.getOfferType() != BookOfferType.EXCHANGE
                || listing.getAvailability() != ListingAvailability.AVAILABLE) {
            throw new IllegalArgumentException("Both books must be available Exchange listings.");
        }
    }

    private void requireOwner(ExchangeRequest exchange, UUID memberId) {
        if (!exchange.getOwner().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only the book owner can accept or decline this request.");
        }
    }

    private void requireParticipant(ExchangeRequest exchange, UUID memberId) {
        if (!exchange.getOwner().getId().equals(memberId) && !exchange.getRequester().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only exchange participants can change this request.");
        }
    }
}
