package com.bookloop.account;

public record PasswordResetCompletionRequest(String token, String newPassword) {
}
