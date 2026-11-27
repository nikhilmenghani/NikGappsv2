# phase05-002-browser — browser test page isolation

Corrects only the separate instrumentation driver's browser targeting. Automatic code pasting requires GitHub's exact `/login/device` path, a device-activation page and no login, two-factor or passkey prompt. Explicit HTTP URLs are rejected. Unknown account or consent still requires the user. Passwords, actual codes and tokens are never returned or captured.

Production OAuth behavior is unchanged; the main APK changes version identity. The previous attempt installed normally and passed synthetic dialog checks but stopped at browser login; see [interrupted observations](INTERRUPTED.md). No real OAuth pass or post-feature scan is claimed for that attempt. Its prior manual-token session remains available.

Verification: release lint, 53 unit tests and fixture compilation. Repeat controlled dialog views and real authorization on OnePlus, then the same APK bytes on Poco only after OnePlus passes. Browser login/authorization remains a user step when required. No new device pass is claimed in advance.
