package com.bookloop.account;

/** Public profile details returned to the signed-in account owner. */
public record AccountProfileResponse(
        String displayName,
        String email,
        String city,
        String pendingEmail,
        boolean emailVerified
) {
}
