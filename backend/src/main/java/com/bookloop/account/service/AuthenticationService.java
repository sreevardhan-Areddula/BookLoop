package com.bookloop.account.service;



import com.bookloop.account.dto.LoginResponse;
import com.bookloop.account.entity.Account;
import com.bookloop.account.entity.IssuedSession;
import com.bookloop.account.repository.AccountRepository;
import com.bookloop.security.entity.RefreshSession;
import com.bookloop.security.repository.RefreshSessionRepository;
import com.bookloop.security.service.AccessTokenService;
import com.bookloop.security.service.TokenHashing;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(7);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final String dummyPasswordHash;

    public AuthenticationService(
            AccountRepository accountRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService
    ) {
        this.accountRepository = accountRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** Checks credentials and email verification, then creates an access and refresh token pair. */
    @Transactional
    public IssuedSession login(String email, String rawPassword) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmail(normalizedEmail).orElse(null);
        String candidateHash = account == null ? dummyPasswordHash : account.passwordHash();
        boolean passwordMatches = rawPassword != null && passwordEncoder.matches(rawPassword, candidateHash);
        if (account == null || !passwordMatches || !account.isEmailVerified()) {
            throw new BadCredentialsException("Email or password is incorrect, or the email is not verified.");
        }

        return createSession(account, UUID.randomUUID());
    }

    /** Rotates a refresh token; replay revokes every token in its session family. */
    @Transactional
    public IssuedSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return null;
        }
        RefreshSession current = refreshSessionRepository
                .findByTokenHashForUpdate(TokenHashing.sha256(rawRefreshToken)).orElse(null);
        if (current == null) {
            return null;
        }
        Instant now = Instant.now();
        if (current.getRevokedAt() != null) {
            refreshSessionRepository.revokeFamily(current.getFamilyId(), now);
            return null;
        }
        if (!current.getExpiresAt().isAfter(now) || !current.getAccount().isEmailVerified()) {
            current.revoke(now, null);
            return null;
        }

        String newRawToken = generateRefreshToken();
        String newHash = TokenHashing.sha256(newRawToken);
        current.revoke(now, newHash);
        refreshSessionRepository.save(current);
        refreshSessionRepository.save(new RefreshSession(
                current.getAccount(), current.getFamilyId(), newHash, now.plus(REFRESH_TOKEN_LIFETIME)
        ));
        return issuedSession(current.getAccount(), newRawToken);
    }

    /** Revokes a refresh token, including all sessions in its family if a rotated token is replayed. */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshSessionRepository.findByTokenHashForUpdate(TokenHashing.sha256(rawRefreshToken))
                .ifPresent(session -> {
                    Instant now = Instant.now();
                    if (session.getRevokedAt() != null) {
                        refreshSessionRepository.revokeFamily(session.getFamilyId(), now);
                    } else {
                        session.revoke(now, null);
                    }
                });
    }

    private IssuedSession createSession(Account account, UUID familyId) {
        String rawRefreshToken = generateRefreshToken();
        refreshSessionRepository.save(new RefreshSession(
                account,
                familyId,
                TokenHashing.sha256(rawRefreshToken),
                Instant.now().plus(REFRESH_TOKEN_LIFETIME)
        ));
        return issuedSession(account, rawRefreshToken);
    }

    private IssuedSession issuedSession(Account account, String rawRefreshToken) {
        LoginResponse.MemberSummary member = new LoginResponse.MemberSummary(
                account.getId(), account.getDisplayName(), account.getEmail(), account.getCity(), account.isEmailVerified()
        );
        return new IssuedSession(accessTokenService.issue(account), rawRefreshToken, member);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
