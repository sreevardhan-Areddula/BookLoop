package com.bookloop.sale;

import com.bookloop.book.BookListingResponse;

import java.time.Instant;
import java.util.UUID;

/** Sale interest visible only to its requester and listing owner. */
public record SaleInterestRequestResponse(
        UUID id,
        SaleInterestStatus status,
        BookListingResponse book,
        UUID requesterId,
        String requesterName,
        UUID ownerId,
        String ownerName,
        boolean incoming,
        Boolean reserveListing,
        Instant createdAt,
        Instant acceptedAt
) {
    /** Includes member-relative direction while mapping a request for the private inbox. */
    public static SaleInterestRequestResponse from(SaleInterestRequest request, UUID viewerId) {
        return new SaleInterestRequestResponse(
                request.getId(), request.getStatus(), BookListingResponse.from(request.getListing()),
                request.getRequester().getId(), request.getRequester().getDisplayName(),
                request.getOwner().getId(), request.getOwner().getDisplayName(),
                request.getOwner().getId().equals(viewerId), request.getReserveListing(),
                request.getCreatedAt(), request.getAcceptedAt()
        );
    }
}
