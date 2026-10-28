# Free-book claim workflow

## Member flow

An authenticated member can request another member's available Free listing from Browse. The listing owner sees incoming claims in **Exchanges → Free claims** and receives an email notification. The in-app badge counts unreviewed incoming claims and unread chat messages.

The owner may accept or decline each pending claim. Multiple requests may be pending at once. Accepting one reserves the listing, removes it from Browse, and closes the other pending claims for that listing. The accepted claimant and owner can then coordinate pickup in a private text-only chat. Chat refreshes every five seconds.

Either participant may cancel a pending or accepted claim. Cancelling an accepted claim releases the listing back to Browse. The owner may mark an accepted pickup handed over; this completes the claim, permanently removes the listing from Browse, and makes the chat read-only. Claims do not expire.


## API

- `POST /api/free-claims` with `{ "listingId": "..." }` creates a pending claim for an available Free listing owned by another member.
- `GET /api/free-claims` returns claims where the authenticated member is requester or owner.
- `POST /api/free-claims/{id}/review` marks an incoming pending claim as viewed.
- `POST /api/free-claims/{id}/accept` accepts an incoming pending claim, reserves the listing, and closes competing pending claims.
- `POST /api/free-claims/{id}/decline` declines an incoming pending claim.
- `POST /api/free-claims/{id}/hand-over` lets the owner complete an accepted pickup and removes the listing from Browse.
- `POST /api/free-claims/{id}/cancel` cancels a participant's pending or accepted claim; cancellation of an accepted claim releases the listing.
- `GET /api/free-claims/unread-count` returns the combined count of unreviewed incoming claims and unread Free-claim messages.
- `GET /api/free-claims/{id}/messages` returns chat history for an accepted, handed-over, or cancelled claim and marks received messages read. An optional `after` timestamp returns newer messages.
- `POST /api/free-claims/{id}/messages` sends a text-only message during an accepted claim. Message text is limited to 2,000 characters.

All routes require member authorization. Only the owner can accept or decline. Only the claim owner and requester can access that claim or its conversation. Duplicate active claims by the same requester for one listing are rejected.

## Data and notifications

Flyway migration `V13__add_free_book_claims.sql` creates `free_claim_requests` and `free_claim_messages`. The owner's email is notified after a new claim is committed. Private messages and unread state are stored in MySQL.
