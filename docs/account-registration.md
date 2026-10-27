# Account Registration Service

`AccountRegistrationService.register` accepts a display name, email, and raw
password. It trims the name, normalizes email to lowercase, validates the fields,
checks for an existing email, hashes the password through the configured
`PasswordEncoder`, and saves a new account. New accounts remain unverified and do
not yet have a city; those steps happen after registration.

The method returns `RegistrationResult`, which contains only the account ID,
display name, email, and verification state. It does not expose the password hash.
The signup endpoint invokes this service and starts email verification for a new
account.

Passwords must currently contain at least 12 characters. The email check is a
basic format check; actual deliverability is established by the separate email
verification flow. Duplicate checking is performed before saving, and the
database unique constraint remains necessary to guard concurrent registrations.
