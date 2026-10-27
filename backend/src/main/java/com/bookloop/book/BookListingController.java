package com.bookloop.book;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class BookListingController {

    private final BookListingService listingService;

    public BookListingController(BookListingService listingService) {
        this.listingService = listingService;
    }

    /** Returns member-visible books filtered by title, genre, and city. */
    @GetMapping
    public List<BookListingResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BookOfferType offerType
    ) {
        return listingService.search(title, genre, city, offerType);
    }

    /** Returns the authenticated member's own book listings. */
    @GetMapping("/mine")
    public List<BookListingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return listingService.mine(UUID.fromString(jwt.getSubject()));
    }

    /** Creates a listing owned by the authenticated member and returns HTTP 201. */
    @PostMapping
    public ResponseEntity<BookListingResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BookListingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                listingService.create(UUID.fromString(jwt.getSubject()), request)
        );
    }

    /** Attaches the authenticated owner's already checked bill to an existing sale listing. */
    @PutMapping("/{id}/sale-verification")
    public BookListingResponse attachSaleVerification(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody SaleVerificationRequest request
    ) {
        return listingService.attachSaleVerification(
                UUID.fromString(jwt.getSubject()), id, request.saleBillDocumentId()
        );
    }

    public record SaleVerificationRequest(UUID saleBillDocumentId) { }
}
