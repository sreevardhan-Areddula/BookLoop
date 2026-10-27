package com.bookloop.sale;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sale-interests")
public class SaleInterestController {

    private final SaleInterestRequestService requestService;

    public SaleInterestController(SaleInterestRequestService requestService) {
        this.requestService = requestService;
    }

    /** Creates a pending Sale interest for the authenticated BookLoop member. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleInterestRequestResponse create(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody CreateSaleInterestRequest request) {
        return requestService.createInterest(UUID.fromString(jwt.getSubject()), request.listingId());
    }

    /** Lists only Sale interest requests involving the authenticated member. */
    @GetMapping
    public List<SaleInterestRequestResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return requestService.listForMember(UUID.fromString(jwt.getSubject()));
    }

    /** Marks one pending request as seen when its seller opens it. */
    @PostMapping("/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        requestService.markViewed(UUID.fromString(jwt.getSubject()), id);
    }

    /** Accepts an interest and records whether the seller reserves the listing. */
    @PostMapping("/{id}/accept")
    public SaleInterestRequestResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              @RequestBody SaleInterestDecisionRequest request) {
        return requestService.acceptInterest(UUID.fromString(jwt.getSubject()), id, request.reserveListing());
    }

    /** Declines an incoming interest request without changing listing availability. */
    @PostMapping("/{id}/decline")
    public SaleInterestRequestResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return requestService.declineInterest(UUID.fromString(jwt.getSubject()), id);
    }

    /** Returns the seller's number of unread incoming Sale interest requests. */
    @GetMapping("/unread-count")
    public SaleInterestUnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return requestService.unreadCount(UUID.fromString(jwt.getSubject()));
    }
}
