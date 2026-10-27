# Sign-up page

The React page at `/signup` collects display name, email, and password and sends them to `POST /api/auth/register`. The browser checks required fields, email format, and the 12-character password minimum; the backend performs authoritative validation and persistence.

The page displays the API's generic response for both new and previously registered email addresses, avoiding account-existence disclosure. After a successful response, it clears the password from component state and asks the member to check email. Email verification happens on `/verify-email`.

After verification, members can sign in. A member without a city is sent through profile completion before entering `/books`. Profile and marketplace pages are implemented; see [implementation status](implementation-status.md) for unfinished marketplace workflows.

The API origin can be overridden with `VITE_API_BASE_URL`; it defaults to `http://localhost:8081` for local development.
