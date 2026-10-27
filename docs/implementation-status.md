# Implementation status

This page describes what the current code supports. Approved product behavior that is not yet implemented remains listed here so the requirements are not mistaken for available functionality.

## Available

- Account registration, email verification, sign-in/out, password recovery, profile completion, profile editing, and verified email changes.
- Browse/search, create Sale/Free/Exchange listings, My Books, and seller bill upload with local OCR price checking, including later verification.
- Exchange request creation, seller review/accept/decline, reservation/conflict closure, cancellation, mutual completion, text chat, unread counts, email notification, and presence.
- Sale interest requests, seller review/accept/decline, optional reservation, closure of other pending interests when reserving, email notification, and an in-app badge.

## Pending

- Private chat for an accepted Sale interest. The Exchanges page currently records acceptance and displays that Sale chat will come in a later step; Exchange chat is already available.
- Seller-controlled sharing of a redacted bill copy inside the Sale chat.
- Seller marking a Sale listing sold, removing it from Browse, and closing other pending interests while preserving conversation history.
- A Free-book claim/handover workflow. Free listings can be browsed but do not yet have a claim action.
- Hosting and deployment.

## Known scope boundaries

- Sale interest acceptance is not a completed sale and does not process payments.
- The OCR tick confirms only that the asking price is below a paid amount read from the uploaded bill. It does not establish bill authenticity.
- Sale bill files are private; the current UI and API do not provide a public bill link or buyer chat attachment flow.

Update this page alongside the relevant workflow guide whenever implementation status changes.
