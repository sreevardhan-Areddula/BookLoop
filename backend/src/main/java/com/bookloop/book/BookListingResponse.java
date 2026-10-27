package com.bookloop.book;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookListingResponse(
        UUID id,
        String title,
        String author,
        List<String> genres,
        BookCondition condition,
        String city,
        BookOfferType offerType,
        BigDecimal askingPrice,
        String isbn,
        String description,
        UUID ownerId,
        String ownerName,
        Instant createdAt,
        ListingAvailability availability,
        boolean salePriceVerified
) {
    public static BookListingResponse from(BookListing listing) {
        return new BookListingResponse(
                listing.getId(), listing.getTitle(), listing.getAuthor(), listing.getGenres(),
                listing.getCondition(), listing.getCity(), listing.getOfferType(), listing.getAskingPrice(),
                listing.getIsbn(), listing.getDescription(), listing.getOwner().getId(),
                listing.getOwner().getDisplayName(), listing.getCreatedAt(), listing.getAvailability(),
                listing.isSalePriceVerified()
        );
    }
}
