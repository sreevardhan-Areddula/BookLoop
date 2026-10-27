# Password Storage

BookLoop uses Spring Security's Argon2 password encoder for account passwords.

The `passwordEncoder()` bean in `PasswordConfiguration` is responsible for providing
the shared encoder used by later account services to create password hashes and
compare login attempts. Argon2 generates a random salt for each encoded password;
only the resulting encoded string should be stored in the account record. Raw
passwords must never be persisted or logged.

The configured parameters are a 16-byte salt, 32-byte output, parallelism 1,
19 MiB memory, and 2 iterations. Bouncy Castle supplies the Argon2 implementation
required by Spring Security. These settings should be performance-checked on the
deployment environment before launch, since password hashing intentionally consumes
CPU and memory.

This configuration only provides password hashing and matching. It does not yet
implement account registration, login, JWT issuance, or password reset.
