package com.bookloop.account.service;



import com.bookloop.account.dto.AccountProfileResponse;
import com.bookloop.account.dto.LoginResponse;
import com.bookloop.account.dto.ProfileCompletionResponse;
import com.bookloop.account.entity.Account;
import com.bookloop.account.repository.AccountRepository;
import com.bookloop.book.repository.BookListingRepository;
import com.bookloop.security.service.AccessTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountProfileService {

    private final AccountRepository accountRepository;
    private final BookListingRepository bookListingRepository;
    private final AccessTokenService accessTokenService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AccountProfileService(AccountRepository accountRepository, BookListingRepository bookListingRepository,
                                 AccessTokenService accessTokenService, PasswordEncoder passwordEncoder,
                                 EmailVerificationService emailVerificationService) {
        this.accountRepository = accountRepository;
        this.bookListingRepository = bookListingRepository;
        this.accessTokenService = accessTokenService;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    /** Loads profile and pending-email details for the authenticated account. */
    @Transactional(readOnly = true)
    public AccountProfileResponse getProfile(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        return new AccountProfileResponse(account.getDisplayName(), account.getEmail(), account.getCity(),
                account.getPendingEmail(), account.isEmailVerified());
    }

    /** Validates and saves the member's name and city, synchronizing all existing listing locations. */
    @Transactional
    public AccountProfileResponse updateProfile(UUID accountId, String displayName, String city) {
        String normalizedName = normalizeRequired(displayName, "Display name", 255);
        String normalizedCity = normalizeRequired(city, "City", 255);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!account.isEmailVerified()) {
            throw new IllegalArgumentException("Verify your email before updating your profile.");
        }

        boolean cityChanged = !normalizedCity.equals(account.getCity());
        account.updateDisplayName(normalizedName);
        account.updateCity(normalizedCity);
        if (cityChanged) {
            bookListingRepository.updateCityForOwner(accountId, normalizedCity);
        }
        accountRepository.save(account);
        return new AccountProfileResponse(account.getDisplayName(), account.getEmail(), account.getCity(),
                account.getPendingEmail(), account.isEmailVerified());
    }

    /** Checks the current password, then stores a pending email and starts its verification flow. */
    @Transactional
    public AccountProfileResponse requestEmailChange(UUID accountId, String currentPassword, String newEmail) {
        String normalizedEmail = normalizeEmail(newEmail);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, account.passwordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Current password is incorrect.");
        }
        if (normalizedEmail.equalsIgnoreCase(account.getEmail())) {
            throw new IllegalArgumentException("This is already your current email address.");
        }
        if (accountRepository.existsByEmail(normalizedEmail)
                || accountRepository.existsByPendingEmailAndIdNot(normalizedEmail, accountId)) {
            throw new IllegalArgumentException("That email address is already in use.");
        }
        account.setPendingEmail(normalizedEmail);
        accountRepository.save(account);
        emailVerificationService.sendEmailChangeVerification(account, normalizedEmail);
        return new AccountProfileResponse(account.getDisplayName(), account.getEmail(), account.getCity(),
                account.getPendingEmail(), account.isEmailVerified());
    }

    /** Saves a required public city and upgrades the limited onboarding access token to member scope. */
    @Transactional
    public ProfileCompletionResponse completeCity(UUID accountId, String city) {
        String normalizedCity = normalizeRequired(city, "City", 255);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (!account.isEmailVerified()) {
            throw new IllegalArgumentException("Verify your email before completing your profile.");
        }

        account.updateCity(normalizedCity);
        accountRepository.save(account);
        LoginResponse.MemberSummary member = new LoginResponse.MemberSummary(
                account.getId(), account.getDisplayName(), account.getEmail(), account.getCity(), account.isEmailVerified()
        );
        return new ProfileCompletionResponse(accessTokenService.issue(account), member);
    }

    /** Trims required profile text and enforces its storage length. */
    private String normalizeRequired(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is required and must be at most " + maxLength + " characters.");
        }
        return normalized;
    }

    /** Normalizes an email address and rejects malformed or overlong input. */
    private String normalizeEmail(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > 320 || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return normalized;
    }
}
