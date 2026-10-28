package com.bookloop.book;

/** Lifecycle state for an available, reserved, exchanged, or manually sold listing. */
public enum ListingAvailability {
    AVAILABLE,
    RESERVED,
    EXCHANGED,
    HANDED_OVER,
    SOLD
}
