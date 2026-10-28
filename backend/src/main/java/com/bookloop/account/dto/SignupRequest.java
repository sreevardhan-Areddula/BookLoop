package com.bookloop.account.dto;


/** Request body accepted by the public signup endpoint. */
public record SignupRequest(String displayName, String email, String password) {
}
