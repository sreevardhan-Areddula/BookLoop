package com.bookloop.book;

import java.time.Instant;
import java.util.UUID;

/** Free-book claim details visible only to the requester and listing owner. */
public record FreeClaimRequestResponse(UUID id, FreeClaimStatus status, BookListingResponse book,
                                       UUID requesterId, String requesterName, UUID ownerId,
                                       String ownerName, boolean incoming, Instant createdAt,
                                       Instant acceptedAt, Instant cancelledAt, UUID cancelledById) {
    public static FreeClaimRequestResponse from(FreeClaimRequest claim, UUID viewerId) {
        return new FreeClaimRequestResponse(claim.getId(), claim.getStatus(),
                BookListingResponse.from(claim.getListing()), claim.getRequester().getId(),
                claim.getRequester().getDisplayName(), claim.getOwner().getId(),
                claim.getOwner().getDisplayName(), claim.getOwner().getId().equals(viewerId),
                claim.getCreatedAt(), claim.getAcceptedAt(), claim.getCancelledAt(),
                claim.getCancelledBy() == null ? null : claim.getCancelledBy().getId());
    }
}
