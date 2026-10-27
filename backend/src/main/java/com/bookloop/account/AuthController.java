package com.bookloop.account;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "BOOKLOOP_REFRESH";
    private static final Duration REFRESH_LIFETIME = Duration.ofDays(7);

    private static final SignupResponse GENERIC_SIGNUP_RESPONSE = new SignupResponse(
            "If signup can proceed, verification instructions will be sent to the provided email."
    );

    private final AccountRegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationService authenticationService;
    private final AccountRecoveryService accountRecoveryService;
    private final boolean secureCookie;

    public AuthController(
            AccountRegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            AuthenticationService authenticationService,
            AccountRecoveryService accountRecoveryService,
            @Value("${bookloop.cookie.secure:false}") boolean secureCookie
    ) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.authenticationService = authenticationService;
        this.accountRecoveryService = accountRecoveryService;
        this.secureCookie = secureCookie;
    }

    /** Creates an unverified account and returns the same response for duplicate emails. */
    @PostMapping("/register")
    public ResponseEntity<SignupResponse> register(@RequestBody SignupRequest request) {
        try {
            RegistrationResult result = registrationService.register(
                    request.displayName(),
                    request.email(),
                    request.password()
            );
            emailVerificationService.sendVerification(result.id());
        } catch (DuplicateAccountException ignored) {
            // Match the successful response so callers cannot enumerate account emails.
        }

        return ResponseEntity.accepted().body(GENERIC_SIGNUP_RESPONSE);
    }

    /** Consumes a valid one-time token and marks its account email as verified. */
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
        return ResponseEntity.noContent().build();
    }

    /** Requests another verification message without revealing whether the email exists. */
    @PostMapping("/resend-verification")
    public ResponseEntity<SignupResponse> resendVerification(
            @RequestBody ResendVerificationRequest request
    ) {
        emailVerificationService.resendVerification(request.email());
        return ResponseEntity.accepted().body(GENERIC_SIGNUP_RESPONSE);
    }

    /** Authenticates a verified account, returns a short-lived access token, and sets a refresh cookie. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        IssuedSession session = authenticationService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(session.loginResponse());
    }

    /** Rotates the refresh cookie and returns a fresh in-memory access token. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        IssuedSession session = authenticationService.refresh(refreshToken);
        if (session == null) {
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                    .build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(session.loginResponse());
    }

    /** Revokes the current refresh session and clears its browser cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken
    ) {
        authenticationService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    /** Requests a password-reset email while keeping the response identical for every email. */
    @PostMapping("/password-reset/request")
    public ResponseEntity<SignupResponse> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        accountRecoveryService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(new SignupResponse(
                "If a verified account exists for that email, password-reset instructions will be sent."
        ));
    }

    /** Completes a password reset with its one-time token and revokes existing sessions. */
    @PostMapping("/password-reset/complete")
    public ResponseEntity<Void> completePasswordReset(@RequestBody PasswordResetCompletionRequest request) {
        accountRecoveryService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(REFRESH_LIFETIME)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
