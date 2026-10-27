# BookLoop authentication

## Account lifecycle

1. `POST /api/auth/register` creates an unverified account. The API returns a generic response to avoid disclosing whether an email is registered.
2. The user follows the one-time email verification link (valid for 24 hours) and confirms it in the frontend.
3. `POST /api/auth/login` accepts email and password only for verified accounts. Passwords are hashed with Argon2id.
4. Login returns a 15-minute JWT in the response body and sets a random seven-day refresh token in an HttpOnly cookie. The browser stores the access token in JavaScript memory only. Refresh tokens are stored in MySQL as SHA-256 hashes and rotated after use.
5. If city is missing, the JWT is limited to `profile:complete`; `PUT /api/accounts/me/city` saves the required public city and returns a member-scoped JWT. Other API routes require member scope.
6. Logout revokes the refresh session and clears its cookie. Refresh and logout require the CSRF token from `GET /api/auth/csrf` in `X-XSRF-TOKEN`.

## Local configuration

Set these values in the ignored `backend/.env.local` file. Never commit it or share credentials in chat.

```properties
BOOKLOOP_JWT_SECRET=<base64-encoded random value with at least 32 bytes>
BOOKLOOP_COOKIE_SECURE=false
BOOKLOOP_FRONTEND_ORIGIN=http://localhost:5173
BOOKLOOP_FRONTEND_URL=http://localhost:5173
```

Use `BOOKLOOP_COOKIE_SECURE=true` behind HTTPS. Keep JWT signing keys private and rotate them deliberately; rotating invalidates outstanding access tokens.

## API response details

- Login failures share a generic response for wrong credentials and unverified accounts.
- Registration, verification resend, and reset request do not disclose account existence.
- Email verification tokens last 24 hours; password-reset tokens last one hour and are single-use.
- Password reset revokes all refresh sessions for that account.
- City is public and required before marketplace member access.

## Sign-in and recovery endpoints

- `POST /api/auth/login` accepts `{ "email": "...", "password": "..." }`; successful response contains the access token and member summary, while the refresh token is set only as an HttpOnly cookie.
- `GET /api/auth/csrf` returns the CSRF value used by the SPA. Send it as `X-XSRF-TOKEN` on refresh and logout.
- `POST /api/auth/refresh` rotates the refresh cookie and returns a new access token and member summary. Reuse of a rotated refresh token revokes the token family.
- `POST /api/auth/logout` revokes the current refresh session and clears its cookie.
- `PUT /api/accounts/me/city` accepts `{ "city": "..." }` with a Bearer token limited to `profile:complete`, and returns a member-scoped access token.
- `POST /api/auth/password-reset/request` accepts `{ "email": "..." }` and always returns a generic HTTP `202` response.
- `POST /api/auth/password-reset/complete` accepts `{ "token": "...", "newPassword": "..." }`. The password must be at least 12 characters. Successful completion returns HTTP `204` and revokes every refresh session for the account.

## Gmail delivery

See [email-verification.md](email-verification.md) for `APP_PASSWORD` and `OAUTH2` configuration. Email verification and password recovery cannot deliver links until one mode is configured. The app deliberately does not print reset or verification links in logs.

## Production follow-up

Refresh-session cleanup/expiry retention, rate limiting across instances, account deletion, and a production security review remain before deployment. Marketplace authorization is implemented for the current account and listing workflows; see [implementation status](implementation-status.md) for unfinished marketplace features.
