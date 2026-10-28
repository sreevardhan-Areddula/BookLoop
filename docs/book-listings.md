# Book listings and discovery

## Member flow

After sign-in and profile-city completion, BookLoop opens `/books`. Its main tabs are **Browse books**, **Upload a book**, and **My Books**. Browse includes offer tabs for All books, Sale, Free, and Exchange, plus search by partial title, exact genre, and partial city. My Books is scoped to the signed-in owner.

New listings require a title, author, at least one genre, condition, and one offer type. The listing owner and public city are taken from the signed-in member profile. ISBN and description are optional. Multiple genres are supported.

The first genre choices are Biography, Children, Fantasy, Fiction, History, Mystery, Non-fiction, Poetry, Romance, Science, Science fiction, Self-help, and Thriller. Conditions are New, Like new, Good, and Fair. Offer types are Sale, Free, and Exchange.

## API

- `GET /api/books?title=&genre=&city=&offerType=` returns matching listings, newest first. Every API route requires member authorization.
- `GET /api/books/mine` returns only the authenticated member's listings.
- `POST /api/books` creates a listing for the authenticated member. It accepts `title`, `author`, `genres`, `condition`, `offerType`, and optional `isbn` and `description`. Sale listings also require `askingPrice`; optional `saleBillDocumentId` links a bill that has passed its OCR price check.
- `PUT /api/books/{id}/sale-verification` accepts `{ "saleBillDocumentId": "..." }` and lets the listing owner attach a bill that passed OCR for the current asking price.
- `POST /api/books/{id}/sold` lets the listing owner mark a Sale listing sold. It disappears from Browse and other pending Sale interests close; accepted conversation history remains available.

## Sale listing verification

Sellers can publish a sale listing without a bill; it appears with **Price unverified**. If a seller uploads a bill during listing creation, OCR must identify a paid amount above the asking price before publication. The owner can also add a bill later from My Books. A successful check marks the listing with **OCR price check passed** and a tick; the bill itself stays private. The tick means only that OCR read a paid amount above the current price and does not authenticate the bill. Owners may not attach another member's bill or a bill that did not pass.

## Free-book claims

Members can request another member's available Free listing. The owner reviews each request and may accept or decline it. Multiple requests can remain pending; accepting one reserves the listing, removes it from Browse, and closes competing pending requests. Acceptance opens a private text chat for arranging pickup. Either participant can cancel an accepted claim, which makes the listing available again and leaves the conversation read-only. A cancelled claim's history stays visible. New requests trigger an owner email, and the Exchanges badge counts unreviewed incoming requests and unread messages. Claims do not expire.

The owner has no action to mark pickup complete yet, so an accepted listing remains reserved unless the claim is cancelled. See [free-claim workflow](free-claim-workflow.md).

## Remaining marketplace work

Sale interest requests, private chat, redacted bill sharing, and manual sold marking are documented in [sale-interest-workflow.md](sale-interest-workflow.md).
