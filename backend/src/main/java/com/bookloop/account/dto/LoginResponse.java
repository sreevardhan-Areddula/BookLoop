package com.bookloop.account.dto;


import java.util.UUID;

/** Contains an access token and only non-secret account fields. */
public record LoginResponse(
        String accessToken,
        long expiresInSeconds,
        MemberSummary member
) {
    public record MemberSummary(UUID id, String displayName, String email, String city, boolean emailVerified) {
    }
}
