package com.bookloop.account.service;



import com.bookloop.account.dto.RegistrationResult;
import com.bookloop.account.entity.Account;
import com.bookloop.account.exception.DuplicateAccountException;
import com.bookloop.account.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AccountRegistrationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountRegistrationService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates signup fields, normalizes the email, hashes the password, and saves
     * an unverified account. City is intentionally completed after email verification.
     */
    @Transactional
    public RegistrationResult register(String displayName, String email, String rawPassword) {
        String normalizedName = displayName == null ? "" : displayName.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Display name is required.");
        }
        if (normalizedEmail.isBlank() || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 12 characters.");
        }
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateAccountException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        Account account = new Account(normalizedName, normalizedEmail, passwordHash);
        return RegistrationResult.from(accountRepository.save(account));
    }
}
