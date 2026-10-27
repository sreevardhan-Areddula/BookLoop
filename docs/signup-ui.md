# Signup Page

The React page at `/signup` collects display name, email, and password and sends
them to `POST /api/auth/register`. The browser checks required fields, email
format, and the 12-character password minimum; the backend remains responsible
for authoritative validation and persistence.

The page displays the API's generic response for both new and previously
registered email addresses. This avoids exposing account existence in the UI.
After a successful response, it clears the password from component state and
shows an email-check message. Verification itself happens on `/verify-email`.

The API origin can be overridden at build or development time with
`VITE_API_BASE_URL`. It defaults to `http://localhost:8081` for local BookLoop
development. Sign-in and city profile completion are not part of this page yet.
