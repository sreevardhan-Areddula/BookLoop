package com.bookloop.exchange;

/** Request and exchange lifecycle states visible to their participants. */
public enum ExchangeStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED,
    EXPIRED,
    CLOSED,
    COMPLETED
}
