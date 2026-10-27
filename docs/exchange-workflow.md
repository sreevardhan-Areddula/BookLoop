# BookLoop exchange workflow

## Member flow

1. A member creates an `EXCHANGE` book listing.
2. From Browse, another member selects **Request exchange** and chooses one of their own available Exchange listings as the offer.
3. The listing owner sees the incoming request in **Exchanges** and receives an email notification for a newly created request. The circular Exchanges badge counts incoming requests that have not been reviewed and unread chat messages.
4. The owner can accept or decline. Accepting reserves both books and closes pending requests that conflict with either listing. The requester and owner can have other pending requests before acceptance.
5. Once accepted, both participants can coordinate in text chat. The Exchanges page polls for new messages every five seconds. Presence is visible only in an exchange conversation; a member is considered online when active within two minutes.
6. Each participant confirms completion. Both books leave Browse only after both have confirmed.
7. Either participant can cancel a pending request or an accepted exchange. Cancelling an accepted exchange releases its books. Competing requests closed during acceptance remain closed; members can create new requests.
8. Pending requests expire after seven days. Chat history remains visible and read-only after cancellation or completion.

Opening an incoming pending request reviews only that request. Opening a conversation retrieves its history and marks messages addressed to the current member as read. The header count refreshes every 15 seconds while Books or Exchanges is open.

## API routes

All routes require a signed-in BookLoop member and a bearer access token.

| Method | Route | Purpose |
| --- | --- | --- |
| `POST` | `/api/exchanges` | Create a pending request with `requestedListingId` and `offeredListingId`. |
| `GET` | `/api/exchanges` | List requests involving the authenticated member. |
| `POST` | `/api/exchanges/{id}/review` | Mark one incoming pending request reviewed when its owner opens it. |
| `GET` | `/api/exchanges/unread-count` | Count unreviewed incoming requests and unread messages. |
| `POST` | `/api/exchanges/{id}/accept` | Accept an incoming request and reserve both books. |
| `POST` | `/api/exchanges/{id}/decline` | Decline an incoming pending request. |
| `POST` | `/api/exchanges/{id}/cancel` | Cancel a pending request or accepted exchange. |
| `POST` | `/api/exchanges/{id}/complete` | Record the participant's completion confirmation. |
| `GET` | `/api/exchanges/{id}/messages` | Read conversation history; optional `after` timestamp fetches newer messages. |
| `POST` | `/api/exchanges/{id}/messages` | Send a text message (up to 2,000 characters). |
| `POST` | `/api/exchanges/{id}/presence` | Record activity and retrieve the other participant's online/last-active status. |

## Listing availability

Exchange listings use `AVAILABLE`, `RESERVED`, and `EXCHANGED` availability states. Browse returns available listings. Acceptance reserves the requested and offered listings; cancellation releases them; mutual completion moves both out of Browse as exchanged.
