# BookLoop product requirements

This document records product decisions approved for the first version of BookLoop. It is a requirements document, not an implementation plan or database schema. Items under **Still to decide** must be settled before the related feature is built.

## Accounts and profiles

- Only account holders can post books, express interest, exchange books, or use other member features.
- Sign-up uses email and password. Email verification and password reset are included.
- Email verification links expire after 24 hours; password-reset links expire after 1 hour. Each link can be used once.
- Verification and password-reset emails use Gmail SMTP. The application supports selectable App Password and OAuth 2.0 authentication modes; local testing uses App Password, while OAuth 2.0 can be configured for deployment. One mode is active per run. SMTP credentials are local secrets, not source-controlled settings.
- Browser authentication uses 15-minute JWT access tokens held only in memory and 7-day refresh tokens in `HttpOnly`, `Secure`, `SameSite` cookies. Refresh tokens rotate and can be revoked; refresh/logout requests receive CSRF protection.
- At sign-up, collect display name, email, and password.
- After sign-up, members verify their email, then complete their profile with a city before accessing BookLoop. A member's city is public.
- Do not collect mobile numbers in the first version.

## Book listings and discovery

- A member can list a book for sale, give it away for free, or offer it for exchange.
- Each listing has one offer mode at a time.
- Listing cards show the owner's username, city, and necessary book details.
- Members can search listings by title, genre, and city.
- A listing may have multiple genres selected from a predefined set.
- The initial genre set is Biography, Children, Fantasy, Fiction, History, Mystery, Non-fiction, Poetry, Romance, Science, Science fiction, Self-help, and Thriller.
- A listing uses the member's public profile city.
- Required book details: title, author, at least one genre, condition, city, and offer mode. Sale listings require a price; the seller may add a purchase bill for OCR verification during creation or later. Description, book photos, and ISBN are optional. The listing username comes from the member account.

## Sale interest and communication

- The book action is **I'm interested**, not **Buy**.
- An interested member sends an interest request to the seller.
- The seller is notified in the app and by email, then can accept or decline.
- On acceptance, the seller may reserve the listing and close other pending interests or keep it available.
- If the seller accepts, a private in-app chat opens for the two members.
- The interaction coordinates interest and communication; in-app payment handling has not been decided.

## Exchange requests

- A member requesting an exchange selects one of their own BookLoop book listings to offer.
- The request goes to the owner of the requested book.
- If the owner accepts, both books are reserved and a private chat opens.
- Only participants in an accepted exchange can see each other's online/last-active status. Last-active time is shown in relative form, such as “5 minutes ago.”
- Either participant can cancel an accepted exchange. Cancellation releases both books.
- Both participants must confirm completion before the exchange is marked complete.

## Sale-price receipt check

- For a book listed for sale, the entered sale price must be lower than the amount paid for the book shown on its purchase bill.
- The seller can submit a bill as a photo, screenshot, or electronic copy.
- OCR reads the bill amount and checks the sale price against it.
- When OCR can read the amount and the price check passes, the listing displays a tick labeled **OCR price check passed**. This label means the amount comparison passed; it does not claim that OCR authenticated the bill.
- The interested buyer checks the bill's authenticity. After the seller accepts the interest request and the private chat opens, the seller can share a redacted copy of the bill in that chat.
- Bills are not displayed publicly on listing cards.

## Still to decide

- Whether a requester may offer only a listing whose mode is **exchange**.
- How the predefined genre list is selected and maintained.
- How redacted bill copies are produced and shared in chat.
- Purchase completion, payment, and listing availability rules for sale and free listings.
- Hosting and deployment.
- Gmail sender address and credentials.
