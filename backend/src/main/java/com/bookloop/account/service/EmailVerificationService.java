package com.bookloop.account.service;



import com.bookloop.account.entity.Account;
import com.bookloop.account.entity.EmailVerificationToken;
import com.bookloop.account.repository.AccountRepository;
import com.bookloop.account.repository.EmailVerificationTokenRepository;
import com.bookloop.account.util.HexFormatSupport;
import com.bookloop.mail.service.GmailEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationService.class);

    private final AccountRepository accountRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final GmailEmailSender emailSender;
    private final String frontendUrl;

    public EmailVerificationService(
            AccountRepository accountRepository,
            EmailVerificationTokenRepository tokenRepository,
            GmailEmailSender emailSender,
            @Value("${bookloop.mail.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    /** Creates a random 24-hour token, stores only its hash, and emails its link. */
    @Transactional
    public void sendVerification(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
        if (account.isEmailVerified()) {
            return;
        }

        issueAndSend(account);
    }

    /** Resends only for an eligible account and enforces a one-minute per-account cooldown. */
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        accountRepository.findByEmail(normalizedEmail).ifPresent(account -> {
            if (account.isEmailVerified()) {
                return;
            }

            Instant cooldownStart = Instant.now().minus(RESEND_COOLDOWN);
            boolean recentlyIssued = tokenRepository
                    .findFirstByAccount_IdOrderByCreatedAtDesc(account.getId())
                    .map(token -> token.getCreatedAt().isAfter(cooldownStart))
                    .orElse(false);
            if (!recentlyIssued) {
                issueAndSend(account);
            }
        });
    }

    /** Issues a verification link to the pending address and alerts the account's current address. */
    @Transactional
    public void sendEmailChangeVerification(Account account, String newEmail) {
        tokenRepository.deleteByAccount_Id(account.getId());
        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        Instant expiresAt = Instant.now().plus(TOKEN_LIFETIME);
        EmailVerificationToken verificationToken = tokenRepository.save(
                new EmailVerificationToken(account, hash(rawToken), expiresAt, newEmail)
        );
        String link = frontendUrl.replaceAll("/$", "") + "/verify-email?token=" + rawToken;
        try {
            emailSender.send(newEmail, "Verify your new BookLoop email",
                    "Use this link to verify your new BookLoop email within 24 hours:\n\n" + link
                            + "\n\nYour current email remains active until verification is complete.");
        } catch (RuntimeException exception) {
            tokenRepository.delete(verificationToken);
            throw new IllegalStateException("Could not send the verification email. Please try again.", exception);
        }
        try {
            emailSender.send(account.getEmail(), "BookLoop email change requested",
                    "A request was made to change the email on your BookLoop account. The new address will not"
                            + " become active unless it is verified. If you did not make this request, change your"
                            + " password immediately.");
        } catch (RuntimeException exception) {
            LOGGER.warn("BookLoop email-change security notice could not be delivered.");
        }
    }

    private void issueAndSend(Account account) {
        UUID accountId = account.getId();

        tokenRepository.deleteByAccount_Id(accountId);
        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        Instant expiresAt = Instant.now().plus(TOKEN_LIFETIME);
        EmailVerificationToken verificationToken = tokenRepository.save(
                new EmailVerificationToken(account, hash(rawToken), expiresAt)
        );

        String link = frontendUrl.replaceAll("/$", "") + "/verify-email?token=" + rawToken;
        try {
            emailSender.send(
                    account.getEmail(),
                    "Verify your BookLoop email",
                    "Use this link to verify your BookLoop email within 24 hours:\n\n" + link
                            + "\n\nIf you did not create this account, you can ignore this message."
            );
        } catch (RuntimeException exception) {
            tokenRepository.delete(verificationToken);
            LOGGER.warn("BookLoop verification email could not be delivered.");
        }
    }

    /** Validates token hash, expiry, and single-use state before verifying the account. */
    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Verification token is invalid or expired.");
        }

        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Verification token is invalid or expired."));
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Verification token is invalid or expired.");
        }

        String verificationEmail = token.getVerificationEmail();
        Account account = token.getAccount();
        if (verificationEmail == null) {
            account.markEmailVerified();
        } else {
            if (account.getPendingEmail() == null
                    || !account.getPendingEmail().equalsIgnoreCase(verificationEmail)) {
                throw new IllegalArgumentException("Verification token is invalid or expired.");
            }
            if (accountRepository.findByEmail(verificationEmail)
                    .filter(existing -> !existing.getId().equals(account.getId())).isPresent()) {
                throw new IllegalArgumentException("That email address is already in use.");
            }
            account.confirmPendingEmail();
        }
        token.markUsed(now);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormatSupport.toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
