package com.bookloop.account.service;



import com.bookloop.account.entity.Account;
import com.bookloop.account.entity.PasswordResetToken;
import com.bookloop.account.repository.AccountRepository;
import com.bookloop.account.repository.PasswordResetTokenRepository;
import com.bookloop.mail.service.GmailEmailSender;
import com.bookloop.security.repository.RefreshSessionRepository;
import com.bookloop.security.service.TokenHashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class AccountRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountRecoveryService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final GmailEmailSender emailSender;
    private final String frontendUrl;

    public AccountRecoveryService(
            AccountRepository accountRepository,
            PasswordResetTokenRepository resetTokenRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            GmailEmailSender emailSender,
            @Value("${bookloop.mail.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.accountRepository = accountRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    /** Requests a one-hour password-reset link without revealing whether the email is registered. */
    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        accountRepository.findByEmail(normalizedEmail).filter(Account::isEmailVerified).ifPresent(account -> {
            Instant cooldownStart = Instant.now().minus(RESEND_COOLDOWN);
            boolean recentlyIssued = resetTokenRepository
                    .findFirstByAccount_IdOrderByCreatedAtDesc(account.getId())
                    .map(token -> token.getCreatedAt().isAfter(cooldownStart))
                    .orElse(false);
            if (!recentlyIssued) {
                issueResetLink(account);
            }
        });
    }

    /** Consumes a valid single-use link, updates the password hash, and revokes refresh sessions. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters.");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Password reset token is invalid or expired.");
        }

        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenHashing.sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Password reset token is invalid or expired."));
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Password reset token is invalid or expired.");
        }

        Account account = token.getAccount();
        account.updatePasswordHash(passwordEncoder.encode(newPassword));
        token.markUsed(now);
        refreshSessionRepository.revokeAllForAccount(account.getId(), now);
    }

    private void issueResetLink(Account account) {
        resetTokenRepository.deleteByAccount_Id(account.getId());
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken token = resetTokenRepository.save(new PasswordResetToken(
                account, TokenHashing.sha256(rawToken), Instant.now().plus(TOKEN_LIFETIME)
        ));
        String link = frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + rawToken;
        try {
            emailSender.send(
                    account.getEmail(),
                    "Reset your BookLoop password",
                    "Use this link to reset your BookLoop password within 1 hour:\n\n" + link
                            + "\n\nIf you did not request a reset, you can ignore this message."
            );
        } catch (RuntimeException exception) {
            resetTokenRepository.delete(token);
            LOGGER.warn("BookLoop password-reset email could not be delivered.");
        }
    }
}
