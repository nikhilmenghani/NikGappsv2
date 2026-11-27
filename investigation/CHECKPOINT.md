# phase04-007 — authenticated registry delivery and request limits

Moves catalog and release metadata to the pinned GitHub repository contents API. A runtime token is required for that exact HTTPS repository scope; authentication is removed from untrusted destinations, including redirected asset hosts. No credential, account UI, account-storage implementation or future feature SDK is included.

Adds bounded transient retries, request-limit observation (including API headers preceding redirects), primary/secondary limit detection and a shared cooldown persisted across restarts. Missing/invalid credentials, ordinary 4xx errors, size limits and server cooldowns are not retried. Existing verified cache fallback remains available.

Verification: release lint and 39 JVM tests covering host/repository/port scoping, missing credentials, cooldown persistence/expiry, malformed retry headers, interruption, retry classification and pinned metadata. The separate same-signature test APK accepts an ephemeral RSA-encrypted runtime credential, removes its encrypted transfer and private key immediately, fetches real pinned Android16/17 metadata, observes API headers and exercises a controlled cooldown. Credentials never appear in test arguments, APKs, source, evidence or screenshots. Normal account sign-in/storage follows in phase05.

Immediate installation, feature checks and manual Play Protect scans follow OnePlus then Poco using the same published main APK bytes. No device pass is claimed in advance.
