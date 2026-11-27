# phase04-003 — catalog JSON library inclusion

Adds kotlinx.serialization JSON 1.11.0, matching the original app. No JSON catalog parser, catalog cache or package-selection UI is included yet. Existing local project JSON still uses Android org.json.

Verification: release lint and unit tests, resolved dependency graph and unchanged merged permissions/initializers. Device checks will verify normal upgrades, local projects, navigation and restart, followed by manual Play Protect scans on OnePlus then Poco. No device pass is claimed in advance.

The preceding network checkpoint required enabling the v2-specific Allow network access setting on Poco after REJECT_ALL was observed. That setting and the successful physical loss/recovery retest are recorded in the ledger. No original-app or global security setting was changed.
