package com.bookloop.sale.controller;



import com.bookloop.sale.dto.CreateSaleInterestRequest;
import com.bookloop.sale.dto.SaleChatMessageRequest;
import com.bookloop.sale.dto.SaleChatMessageResponse;
import com.bookloop.sale.dto.SaleInterestDecisionRequest;
import com.bookloop.sale.dto.SaleInterestRequestResponse;
import com.bookloop.sale.dto.SaleInterestUnreadCountResponse;
import com.bookloop.sale.entity.SaleSharedBillFile;
import com.bookloop.sale.service.SaleChatService;
import com.bookloop.sale.service.SaleInterestRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/api/sale-interests")
public class SaleInterestController {

    private final SaleInterestRequestService requestService;
    private final SaleChatService chatService;

    public SaleInterestController(SaleInterestRequestService requestService, SaleChatService chatService) {
        this.requestService = requestService;
        this.chatService = chatService;
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

    /** Lists accepted Sale chat history and marks the participant's received messages as read. */
    @GetMapping("/{id}/messages")
    public List<SaleChatMessageResponse> messages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                  @RequestParam(required = false) Instant after) {
        return chatService.listSaleMessages(UUID.fromString(jwt.getSubject()), id, after);
    }

    /** Sends one private text message to the other participant in an accepted Sale interest. */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleChatMessageResponse sendMessage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                               @RequestBody SaleChatMessageRequest request) {
        return chatService.sendSaleMessage(UUID.fromString(jwt.getSubject()), id, request.text());
    }

    /** Lets only the seller upload a redacted bill copy into an accepted private Sale conversation. */
    @PostMapping(value = "/{id}/messages/bill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SaleChatMessageResponse shareBill(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                             @RequestParam("file") MultipartFile file) {
        return chatService.shareRedactedBill(UUID.fromString(jwt.getSubject()), id, file);
    }

    /** Downloads one private shared bill copy for either participant in the accepted Sale conversation. */
    @GetMapping("/{id}/messages/{messageId}/bill")
    public ResponseEntity<byte[]> downloadBill(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                               @PathVariable UUID messageId) {
        SaleSharedBillFile file = chatService.downloadSharedBill(UUID.fromString(jwt.getSubject()), id, messageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.bytes().length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.bytes());
    }
}
