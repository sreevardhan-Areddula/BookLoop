package com.bookloop.account;

/** Request body used to consume an email verification link token. */
public record VerifyEmailRequest(String token) {
}
