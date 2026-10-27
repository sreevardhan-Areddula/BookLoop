package com.bookloop.sale;

/** Post-commit details used to email the owner about a new sale interest request. */
public record NewSaleInterestEvent(String ownerEmail, String requesterName, String listingTitle) { }
