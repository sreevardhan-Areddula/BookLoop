package com.bookloop.exchange.entity;


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
