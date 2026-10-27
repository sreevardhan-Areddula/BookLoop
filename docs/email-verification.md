# Email Verification

## Flow

After a new account is created, `EmailVerificationService.sendVerification`
creates a cryptographically random token with a 24-hour lifetime. Only the
SHA-256 hash is stored in `email_verification_tokens`; the raw token is sent in
the link and is never persisted. Issuing another token invalidates earlier
tokens for that account.

The `/verify-email` frontend page removes the token from the visible URL and
waits for the user to click **Verify email**. It then posts the token to
`POST /api/auth/verify-email`. The backend checks that the token exists, is not
expired, and has not been used, then marks both the token and account verified.

## Gmail configuration

Set these environment variables in the backend's local environment. Do not put
credentials in tracked properties files or share them in chat.

For Gmail App Password mode:

- `BOOKLOOP_MAIL_MODE=APP_PASSWORD`
- `BOOKLOOP_MAIL_USERNAME` to the Gmail sender address
- `BOOKLOOP_GMAIL_APP_PASSWORD` to the Gmail App Password
- `BOOKLOOP_MAIL_FROM` optionally to the sender address (defaults to username)

For Gmail OAuth2 mode:

- `BOOKLOOP_MAIL_MODE=OAUTH2`
- `BOOKLOOP_MAIL_USERNAME` to the Gmail sender address
- `BOOKLOOP_GMAIL_OAUTH_CLIENT_ID`
- `BOOKLOOP_GMAIL_OAUTH_CLIENT_SECRET`
- `BOOKLOOP_GMAIL_OAUTH_REFRESH_TOKEN`

The OAuth2 mode exchanges the refresh token for a short-lived access token when
sending mail and uses Gmail SMTP XOAUTH2. Select one mode per backend run.

Set `BOOKLOOP_FRONTEND_URL` to the frontend base URL used in verification links
and `BOOKLOOP_FRONTEND_ORIGIN` to the exact origin allowed for API calls. Both
default to `http://localhost:5173` for local development.
The backend listens on `http://localhost:8081` by default; set
`VITE_API_BASE_URL` to that backend origin for local frontend requests.

## Endpoints

- `POST /api/auth/register` creates an unverified account and sends the
  verification message for a newly registered email. Existing-email requests
  receive the same generic HTTP `202` response and do not trigger another email.
- `POST /api/auth/verify-email` accepts `{ "token": "..." }` and returns HTTP
  `204` after successful verification. Invalid, expired, or used tokens return
  HTTP `400` with a generic error.
- `POST /api/auth/resend-verification` accepts `{ "email": "..." }` and returns
  the same generic HTTP `202` response for every address. Unverified accounts
  can receive a new link at most once per minute.

Gmail credentials must be configured before registration can deliver mail.
