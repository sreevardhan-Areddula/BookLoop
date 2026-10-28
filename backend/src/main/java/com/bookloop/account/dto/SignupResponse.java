package com.bookloop.account.dto;


/** Generic signup response used to avoid confirming whether an email is registered. */
public record SignupResponse(String message) {
}
