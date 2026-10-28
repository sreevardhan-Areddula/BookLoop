# Sale interest requests

## Member flow

1. A signed-in member selects **I'm interested** on another member's available Sale listing.
2. The seller receives an in-app badge and an email notification. Requests are available in the **Sale interests** tab on the Exchanges page.
3. The seller may accept while reserving the listing, which removes it from Browse and closes other pending interests, or accept while leaving it available.
4. The seller may decline a pending request. A buyer with a pending or accepted request cannot send another active request for the same listing.
5. Once accepted, a private chat opens for the buyer and seller. Participants can send text messages; the chat polls for new messages every five seconds.
6. The seller may choose to upload a redacted bill copy in the chat. The seller must redact it before upload. Copies are limited to JPEG, PNG, or PDF files up to 5 MB; PDF copies may contain up to 10 pages. Only the two participants can download the shared copy.
7. The seller can mark the listing sold from My Books or the accepted interest. The listing leaves Browse and other pending interests close. Accepted chat history remains available.

Requests and conversations are private to the requester and listing owner. A seller opening a pending request marks it reviewed. The header badge includes unreviewed incoming requests and unread Sale messages. Requests do not expire in this version. Email delivery failure does not remove the in-app request.

The app stores a seller-shared copy as a separate private file; it does not modify or redact the seller's file. The seller is responsible for checking the copy before sharing. The original purchase bill uploaded for OCR remains private and is not shared automatically.

## API routes

All routes require a signed-in member and bearer access token.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/sale-interests` | Create a request with `listingId`. |
| `GET` | `/api/sale-interests` | List requests involving the authenticated member. |
| `POST` | `/api/sale-interests/{id}/review` | Mark an incoming pending request reviewed. |
| `POST` | `/api/sale-interests/{id}/accept` | Accept with `{ "reserveListing": true|false }`. |
| `POST` | `/api/sale-interests/{id}/decline` | Decline an incoming pending request. |
| `GET` | `/api/sale-interests/unread-count` | Count unreviewed incoming requests and unread Sale chat messages. |
| `GET` | `/api/sale-interests/{id}/messages` | Read accepted conversation history; optional `after` timestamp fetches newer messages and marks incoming messages read. |
| `POST` | `/api/sale-interests/{id}/messages` | Send a text message (up to 2,000 characters). |
| `POST` | `/api/sale-interests/{id}/messages/bill` | Seller uploads a redacted JPEG, PNG, or PDF copy into the chat. |
| `GET` | `/api/sale-interests/{id}/messages/{messageId}/bill` | Download a shared copy as one of the two participants. |
| `POST` | `/api/books/{id}/sold` | Seller marks a Sale listing sold; pending interests close. |

The shared Exchanges header badge combines unread Exchange activity, unreviewed Sale interest requests, and unread Sale messages. An accepted Sale interest does not imply payment or a completed sale. Sellers mark an offline sale manually. Payments are not handled in BookLoop.
