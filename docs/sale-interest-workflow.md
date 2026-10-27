# Sale interest requests

## Member flow

1. A signed-in member selects **I'm interested** on another member's available Sale listing.
2. The seller receives an in-app badge and an email notification. Requests are available in the **Sale interests** tab on the Exchanges page.
3. The seller may accept while reserving the listing, which removes it from Browse and closes other pending interests, or accept while leaving it available.
4. The seller may decline a pending request. A buyer with a pending or accepted request cannot send another active request for the same listing.
5. Request acceptance records the response but does not yet open a conversation. Sale chat is a separate implementation task.

Requests are private to the requester and listing owner. A seller opening a pending request marks it reviewed and clears it from the new-request badge count. Requests are not given an expiry deadline in this slice. Email delivery failure does not remove the in-app request.

## API routes

All routes require a signed-in member and bearer access token.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/sale-interests` | Create a request with `listingId`. |
| `GET` | `/api/sale-interests` | List requests involving the authenticated member. |
| `POST` | `/api/sale-interests/{id}/review` | Mark an incoming pending request reviewed. |
| `POST` | `/api/sale-interests/{id}/accept` | Accept with `{ "reserveListing": true|false }`. |
| `POST` | `/api/sale-interests/{id}/decline` | Decline an incoming pending request. |
| `GET` | `/api/sale-interests/unread-count` | Count unread incoming pending requests. |

The shared Exchanges header badge combines unread exchange activity and unread Sale interest requests. An accepted Sale interest does not imply payment or a completed sale. Sellers still mark a completed offline sale separately in a later feature.
