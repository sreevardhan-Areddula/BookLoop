package com.bookloop.sale;

/** Private file bytes and response headers for a bill shared inside one Sale conversation. */
public record SaleSharedBillFile(String filename, String contentType, byte[] bytes) { }
