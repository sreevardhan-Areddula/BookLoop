package com.bookloop.account.dto;


public record PasswordResetCompletionRequest(String token, String newPassword) {
}
