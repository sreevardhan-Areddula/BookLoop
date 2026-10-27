package com.bookloop.exchange;

import java.util.UUID;

/** IDs for the requested book and the member's proposed exchange book. */
public record CreateExchangeRequest(UUID requestedListingId, UUID offeredListingId) {
}
