# phase05-002 — GitHub browser/device-code sign-in

Adds GitHub account sign-in through the original app's existing public OAuth client identifier. The device flow uses GitHub's fixed HTTPS endpoints without a client secret or embedded credentials. It honors polling intervals, slow-down responses, expiry, cancellation, transient failures and request limits. The returned token is used to verify the account before replacing the encrypted local session; failed/cancelled sign-in preserves the existing account.

The Account settings row opens the shared-style glass dialog with a divider, 16dp buttons and a scrollable body. The one-time code is copied as sensitive clipboard content and GitHub opens in the browser. Automatic session revalidation/logout, required account gating, avatar popup and expanded instructions remain later checkpoints.

Verification: release lint and 53 JVM tests, including protocol timing, long slow-down/expiry, cancellation, denial, transient retry, safe code logging and post-authorization profile retry. A host preflight confirmed that the existing public OAuth client has device flow enabled; its temporary codes were discarded without logging. No new dependencies or manifest permissions are introduced.

Separate instrumentation renders synthetic TEST-CODE dialogs in light/dark/classic mode without opening a browser or changing the session, then exercises the real browser flow. Actual authorization codes, tokens and password fields are not captured or dumped. Only explicitly recognized consent for the previously configured account can be approved automatically; browser login or unverified account/consent requires the user. Device success and immediate scans must be observed on OnePlus before Poco; no pass is claimed in advance.

Reference: [GitHub device authorization flow](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow).
