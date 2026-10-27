# Profile management

Members open `/profile` from the round initials button beside **Sign out** in the Browse header. The page displays the current display name, email, and public city.

## Name and city

The **Save changes** form updates display name and city together. When city changes, BookLoop updates the city on every existing listing owned by that member, so listing search and profile location remain consistent.

## Email changes

Email changes use a separate form and require the current password. The requested address is stored as pending and does not replace the active sign-in email yet. BookLoop sends a one-time, 24-hour verification link to the new address and a security notice to the current address. After the new address is verified, it replaces the active email. If verification email delivery fails, the request is not retained.

## API

- `GET /api/accounts/me` returns the authenticated member's profile, including any pending email.
- `PUT /api/accounts/me/profile` updates display name and city.
- `POST /api/accounts/me/email-change` accepts `currentPassword` and `newEmail` and starts verification.
- `POST /api/auth/verify-email` verifies signup addresses or completes a pending profile email change, depending on the token.

The profile endpoints require member authorization. Verification tokens are random, single-use, and stored as SHA-256 hashes. The current email remains unique and active until verification promotes the pending address.
