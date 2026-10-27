# Signup API

## `POST /api/auth/register`

Accepts JSON with `displayName`, `email`, and `password`. The endpoint delegates
validation, email normalization, duplicate detection, Argon2 password hashing,
and account persistence to `AccountRegistrationService.register`.

On a new account or an already-registered email, it responds with HTTP `202` and
the same generic message. This prevents the endpoint from confirming whether an
email belongs to an existing account. For new accounts it sends an email
verification link; invalid signup fields return HTTP `400` with a problem
response.

The endpoint creates an unverified account. Email verification is completed at
`POST /api/auth/verify-email`; see [email-verification.md](email-verification.md)
for token behavior and required Gmail configuration. The generic `202` response
does not confirm that an email was delivered or verified.
