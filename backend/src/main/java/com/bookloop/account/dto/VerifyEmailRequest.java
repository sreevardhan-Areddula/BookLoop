package com.bookloop.account.dto;


/** Request body used to consume an email verification link token. */
public record VerifyEmailRequest(String token) {
}
