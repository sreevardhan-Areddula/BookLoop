package com.bookloop.account.dto;


/** Request body for requesting another email verification message. */
public record ResendVerificationRequest(String email) {
}
