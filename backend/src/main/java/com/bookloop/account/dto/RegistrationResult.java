package com.bookloop.account.dto;



import com.bookloop.account.entity.Account;
import java.util.UUID;

/** Safe account details returned after registration; never includes password data. */
public record RegistrationResult(
        UUID id,
        String displayName,
        String email,
        boolean emailVerified
) {
    public static RegistrationResult from(Account account) {
        return new RegistrationResult(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.isEmailVerified()
        );
    }
}
