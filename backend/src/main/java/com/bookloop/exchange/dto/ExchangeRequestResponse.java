package com.bookloop.exchange.dto;



import com.bookloop.book.dto.BookListingResponse;
import com.bookloop.exchange.entity.ExchangeRequest;
import com.bookloop.exchange.entity.ExchangeStatus;
import java.time.Instant;
import java.util.UUID;

/** Exchange state returned only to one of its two participants. */
public record ExchangeRequestResponse(
        UUID id,
        ExchangeStatus status,
        BookListingResponse requestedBook,
        BookListingResponse offeredBook,
        UUID requesterId,
        String requesterName,
        UUID ownerId,
        String ownerName,
        UUID cancelledById,
        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt,
        Instant cancelledAt,
        Instant requesterCompletedAt,
        Instant ownerCompletedAt,
        Instant completedAt,
        boolean incoming
) {
    /** Maps the request and member-specific direction for the exchange list UI. */
    public static ExchangeRequestResponse from(ExchangeRequest exchange, UUID viewerId) {
        return new ExchangeRequestResponse(
                exchange.getId(), exchange.getStatus(),
                BookListingResponse.from(exchange.getRequestedListing()),
                BookListingResponse.from(exchange.getOfferedListing()),
                exchange.getRequester().getId(), exchange.getRequester().getDisplayName(),
                exchange.getOwner().getId(), exchange.getOwner().getDisplayName(),
                exchange.getCancelledBy() == null ? null : exchange.getCancelledBy().getId(),
                exchange.getCreatedAt(), exchange.getExpiresAt(), exchange.getAcceptedAt(),
                exchange.getCancelledAt(), exchange.getRequesterCompletedAt(), exchange.getOwnerCompletedAt(),
                exchange.getCompletedAt(), exchange.getOwner().getId().equals(viewerId)
        );
    }
}
