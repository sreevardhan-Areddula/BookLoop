package com.bookloop.account.controller;



import com.bookloop.account.dto.AccountProfileResponse;
import com.bookloop.account.dto.CityRequest;
import com.bookloop.account.dto.EmailChangeRequest;
import com.bookloop.account.dto.ProfileCompletionResponse;
import com.bookloop.account.dto.ProfileUpdateRequest;
import com.bookloop.account.service.AccountProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/me")
public class AccountProfileController {

    private final AccountProfileService accountProfileService;

    public AccountProfileController(AccountProfileService accountProfileService) {
        this.accountProfileService = accountProfileService;
    }

    /** Returns profile fields for the authenticated account owner. */
    @org.springframework.web.bind.annotation.GetMapping
    public AccountProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return accountProfileService.getProfile(UUID.fromString(jwt.getSubject()));
    }

    /** Updates the authenticated member's display name and public city. */
    @PutMapping("/profile")
    public AccountProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                @RequestBody ProfileUpdateRequest request) {
        return accountProfileService.updateProfile(UUID.fromString(jwt.getSubject()),
                request.displayName(), request.city());
    }

    /** Starts an email change after checking the current password. */
    @PostMapping("/email-change")
    public AccountProfileResponse requestEmailChange(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestBody EmailChangeRequest request) {
        return accountProfileService.requestEmailChange(UUID.fromString(jwt.getSubject()),
                request.currentPassword(), request.newEmail());
    }

    /** Completes the city required before marketplace member access is granted. */
    @PutMapping("/city")
    public ResponseEntity<ProfileCompletionResponse> completeCity(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CityRequest request
    ) {
        return ResponseEntity.ok(accountProfileService.completeCity(
                UUID.fromString(jwt.getSubject()), request.city()
        ));
    }
}
