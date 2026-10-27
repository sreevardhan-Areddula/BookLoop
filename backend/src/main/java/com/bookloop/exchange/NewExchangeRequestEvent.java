package com.bookloop.exchange;

/** Post-commit data used to notify a listing owner about a new exchange request. */
public record NewExchangeRequestEvent(String ownerEmail, String requesterName,
                                      String requestedTitle, String offeredTitle) {
}
