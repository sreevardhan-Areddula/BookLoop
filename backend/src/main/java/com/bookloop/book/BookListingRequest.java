package com.bookloop.book;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BookListingRequest(
        String title,
        String author,
        List<String> genres,
        BookCondition condition,
        BookOfferType offerType,
        BigDecimal askingPrice,
        String isbn,
        String description,
        UUID saleBillDocumentId
) {
}
