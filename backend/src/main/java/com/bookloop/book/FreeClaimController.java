package com.bookloop.book;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Authenticated API for Free-book claim requests and their private conversations. */
@RestController
@RequestMapping("/api/free-claims")
public class FreeClaimController {

    private final FreeClaimService claimService;

    public FreeClaimController(FreeClaimService claimService) { this.claimService = claimService; }

    /** Creates a pending claim for the authenticated member and returns HTTP 201. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FreeClaimRequestResponse create(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody CreateFreeClaimRequest request) {
        return claimService.createClaim(UUID.fromString(jwt.getSubject()), request.listingId());
    }

    /** Lists only Free claims where the signed-in member is requester or owner. */
    @GetMapping
    public List<FreeClaimRequestResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return claimService.listForMember(UUID.fromString(jwt.getSubject()));
    }

    /** Marks one incoming pending claim seen when its owner opens it. */
    @PostMapping("/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        claimService.markViewed(UUID.fromString(jwt.getSubject()), id);
    }

    /** Lets the listing owner accept a pending claim and reserve the Free book. */
    @PostMapping("/{id}/accept")
    public FreeClaimRequestResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return claimService.acceptClaim(UUID.fromString(jwt.getSubject()), id);
    }

    /** Lets the listing owner decline a pending claim without reserving the book. */
    @PostMapping("/{id}/decline")
    public FreeClaimRequestResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return claimService.declineClaim(UUID.fromString(jwt.getSubject()), id);
    }

    /** Lets the listing owner record that the reserved Free book was handed over. */
    @PostMapping("/{id}/hand-over")
    public FreeClaimRequestResponse handOver(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return claimService.handOverClaim(UUID.fromString(jwt.getSubject()), id);
    }

    /** Lets either participant cancel a pending or accepted claim. */
    @PostMapping("/{id}/cancel")
    public FreeClaimRequestResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return claimService.cancelClaim(UUID.fromString(jwt.getSubject()), id);
    }

    /** Counts unreviewed incoming claims plus unread Free-claim messages for the header badge. */
    @GetMapping("/unread-count")
    public FreeClaimUnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return claimService.unreadCount(UUID.fromString(jwt.getSubject()));
    }

    /** Reads accepted or cancelled conversation history and marks incoming messages read. */
    @GetMapping("/{id}/messages")
    public List<FreeClaimMessageResponse> messages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                   @RequestParam(required = false) Instant after) {
        return claimService.listMessages(UUID.fromString(jwt.getSubject()), id, after);
    }

    /** Sends one text message in an active owner-approved Free claim conversation. */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public FreeClaimMessageResponse sendMessage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                 @RequestBody FreeClaimMessageRequest request) {
        return claimService.sendMessage(UUID.fromString(jwt.getSubject()), id, request.text());
    }
}
