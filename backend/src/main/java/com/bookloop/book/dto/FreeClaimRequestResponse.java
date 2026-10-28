package com.bookloop.book.dto;



import com.bookloop.book.entity.FreeClaimRequest;
import com.bookloop.book.entity.FreeClaimStatus;
import java.time.Instant;
import java.util.UUID;

/** Free-book claim details visible only to the requester and listing owner. */
public record FreeClaimRequestResponse(UUID id, FreeClaimStatus status, BookListingResponse book,
                                       UUID requesterId, String requesterName, UUID ownerId,
                                       String ownerName, boolean incoming, Instant createdAt,
                                       Instant acceptedAt, Instant handedOverAt, Instant cancelledAt, UUID cancelledById) {
    public static FreeClaimRequestResponse from(FreeClaimRequest claim, UUID viewerId) {
        return new FreeClaimRequestResponse(claim.getId(), claim.getStatus(),
                BookListingResponse.from(claim.getListing()), claim.getRequester().getId(),
                claim.getRequester().getDisplayName(), claim.getOwner().getId(),
                claim.getOwner().getDisplayName(), claim.getOwner().getId().equals(viewerId),
                claim.getCreatedAt(), claim.getAcceptedAt(), claim.getHandedOverAt(), claim.getCancelledAt(),
                claim.getCancelledBy() == null ? null : claim.getCancelledBy().getId());
    }
}
