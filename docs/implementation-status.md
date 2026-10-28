# Implementation status

This page describes what the current code supports. Approved product behavior that is not yet implemented remains listed here so the requirements are not mistaken for available functionality.

## Available

- Account registration, email verification, sign-in/out, password recovery, profile completion, profile editing, and verified email changes.
- Browse/search, create Sale/Free/Exchange listings, My Books, and seller bill upload with local OCR price checking, including later verification.
- Exchange request creation, seller review/accept/decline, reservation/conflict closure, cancellation, mutual completion, text chat, unread counts, email notification, and presence.
- Sale interest requests, seller review/accept/decline, optional reservation, closure of other pending interests when reserving, email notification, and an in-app badge.
- Private text chat after Sale-interest acceptance, seller-selected redacted bill sharing, and manual Sale listing sold status. Shared copies are private to the buyer and seller; the seller must redact the file before upload.
- Free-book claim requests, owner acceptance/decline, reservation and competing-claim closure, private text chat, email notification, unread badge, and participant cancellation with read-only chat history.

## Pending

- Marking a Free-book pickup as handed over or otherwise completed. An accepted claim reserves the listing until either participant cancels it; the pickup completion decision is still open.
- Hosting and deployment.

## Known scope boundaries

- Sale interest acceptance is not a completed sale and does not process payments.
- The OCR tick confirms only that the asking price is below a paid amount read from the uploaded bill. It does not establish bill authenticity.
- Sale bill files are private; the current UI and API do not provide a public bill link or buyer chat attachment flow.
- Free claims do not expire. Owners must accept or decline each claim; accepting one closes other pending claims and reserves the listing. There is no pickup completion action yet.

Update this page alongside the relevant workflow guide whenever implementation status changes.
