package com.bookloop.account;

/** Request body for requesting another email verification message. */
public record ResendVerificationRequest(String email) {
}
