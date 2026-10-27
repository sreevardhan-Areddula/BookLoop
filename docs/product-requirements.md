# BookLoop product requirements

This document records approved first-version product behavior. It describes intended behavior; see [implementation status](implementation-status.md) to distinguish features already available from approved work still pending.

## Accounts and profiles

- Only verified account holders who have completed the required profile can use member features.
- Sign-up uses email and password. Members verify their email before signing in; password reset is available.
- At sign-up, collect display name, email, and password. After verification, require a public city. Phone numbers are not collected.
- Members can edit their display name and city. Updating a city also updates their existing listings.
- Email changes require the current password, notify the old address, and become active only after the new address is verified.
- Browser authentication uses in-memory JWT access tokens and rotating refresh tokens in `HttpOnly` cookies, with CSRF protection for refresh and logout.

## Listings and discovery

- A member can post each book as one of: Sale, Free, or Exchange.
- Listing cards show the owner's display name, public city, and book details.
- Members can search by title, genre, and city. A listing can have multiple genres.
- Required book details are title, author, at least one genre, condition, and offer type. Sale listings require an asking price. Description and ISBN are optional.
- The listing city comes from the member profile.
- Sale price is seller-set; the first version does not process payment.
- Free-book claim and fulfillment behavior remains to be implemented.

## Sale listings

- Buyers choose **I'm interested** instead of purchasing in the app. An interest request goes to the seller.
- Sellers can accept or decline. At acceptance, the seller chooses whether to reserve the listing. Reserving removes it from Browse and closes other pending interests; keeping it listed leaves it available.
- A seller may list without a bill. Such listings are marked unverified. A seller may upload a bill during listing creation or later. OCR grants a tick only when it reads a paid amount greater than the current asking price.
- Bills remain private unless the seller manually shares a redacted copy in the private buyer/seller chat.
- After acceptance, a private chat opens between the buyer and seller. The seller manually marks the book sold after an offline sale; this removes it from Browse and closes other pending interests while preserving chat history.
- OCR only checks the numeric comparison; the buyer is responsible for verifying bill authenticity.

## Exchanges

- A requester selects one of their own available Exchange listings as the offer for another member's available Exchange listing.
- The requested-book owner can accept or decline. Acceptance reserves both books and closes conflicting pending requests. Multiple pending requests are allowed before acceptance.
- Both members must confirm completion. Only then do both books leave Browse as exchanged.
- Either participant can cancel a pending request or accepted exchange. Canceling an accepted exchange releases both books.
- Accepted exchange participants can use private, text-only chat and see online/last-active status. Presence is considered online for two minutes after activity.
- Pending exchange requests expire after seven days. Exchange history remains visible but read-only after cancellation or completion.

## Notifications and language

- The Exchanges entry has an in-app badge for unreviewed incoming requests and unread messages. New exchange requests also trigger email notification.
- The initial interface is English-only.

## Deferred decisions

- Hosting and production deployment.
- Payment handling and sale completion beyond manually marking a listing sold.
- Free-book claim, handover, and completion workflow.
