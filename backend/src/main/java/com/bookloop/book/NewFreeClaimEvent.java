package com.bookloop.book;

/** Post-commit data used to email the owner about a new Free-book claim. */
public record NewFreeClaimEvent(String ownerEmail, String requesterName, String bookTitle) { }
