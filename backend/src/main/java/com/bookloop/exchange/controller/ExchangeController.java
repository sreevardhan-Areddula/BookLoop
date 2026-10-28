package com.bookloop.exchange.controller;



import com.bookloop.exchange.dto.ChatMessageResponse;
import com.bookloop.exchange.dto.CreateExchangeRequest;
import com.bookloop.exchange.dto.ExchangeRequestResponse;
import com.bookloop.exchange.dto.PresenceResponse;
import com.bookloop.exchange.dto.SendMessageRequest;
import com.bookloop.exchange.dto.UnreadCountResponse;
import com.bookloop.exchange.service.ExchangeRequestService;
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

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/api/exchanges")
public class ExchangeController {

    private final ExchangeRequestService exchangeService;

    public ExchangeController(ExchangeRequestService exchangeService) {
        this.exchangeService = exchangeService;
    }

    /** Creates a pending proposal from the requested listing and the member's offered listing. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRequestResponse create(@AuthenticationPrincipal Jwt jwt,
                                          @RequestBody CreateExchangeRequest request) {
        return exchangeService.createRequest(UUID.fromString(jwt.getSubject()),
                request.requestedListingId(), request.offeredListingId());
    }

    /** Lists exchanges involving only the authenticated member. */
    @GetMapping
    public List<ExchangeRequestResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return exchangeService.listForMember(UUID.fromString(jwt.getSubject()));
    }

    /** Marks a single incoming request reviewed when its owner opens it. */
    @PostMapping("/{id}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        exchangeService.markRequestViewed(UUID.fromString(jwt.getSubject()), id);
    }

    /** Accepts a received request and reserves its two book listings. */
    @PostMapping("/{id}/accept")
    public ExchangeRequestResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exchangeService.acceptRequest(UUID.fromString(jwt.getSubject()), id);
    }

    /** Declines a received request. */
    @PostMapping("/{id}/decline")
    public ExchangeRequestResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exchangeService.declineRequest(UUID.fromString(jwt.getSubject()), id);
    }

    /** Cancels the member's pending request or accepted exchange. */
    @PostMapping("/{id}/cancel")
    public ExchangeRequestResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exchangeService.cancelRequest(UUID.fromString(jwt.getSubject()), id);
    }

    /** Confirms physical completion for one participant; both confirmations are required to finish. */
    @PostMapping("/{id}/complete")
    public ExchangeRequestResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exchangeService.confirmCompletion(UUID.fromString(jwt.getSubject()), id);
    }

    /** Returns the participant-specific Exchanges badge count. */
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return exchangeService.unreadCount(UUID.fromString(jwt.getSubject()));
    }

    /** Sends a text message in an active accepted exchange. */
    @PostMapping("/{id}/messages")
    public ChatMessageResponse sendMessage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @RequestBody SendMessageRequest request) {
        return exchangeService.sendMessage(UUID.fromString(jwt.getSubject()), id, request.text());
    }

    /** Polls message history and marks messages addressed to the current member as read. */
    @GetMapping("/{id}/messages")
    public List<ChatMessageResponse> messages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              @RequestParam(required = false) Instant after) {
        return exchangeService.listMessages(UUID.fromString(jwt.getSubject()), id, after);
    }

    /** Records the current member's chat activity and returns the other participant's presence. */
    @PostMapping("/{id}/presence")
    public PresenceResponse presence(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exchangeService.recordPresence(UUID.fromString(jwt.getSubject()), id);
    }
}
