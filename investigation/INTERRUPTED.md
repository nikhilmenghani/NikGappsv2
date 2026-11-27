# Interrupted device checks

## phase05-002

Source `439700cf455875bc125ee0fca34dd38aac1e70c7`; main APK SHA-256 `d14784d1a349f759334f99b687252efb5bd737796391e2c304295c4cb3d433eb`.

OnePlus installed the exact published APK normally. Controlled light/dark/classic dialogs and synthetic pending-code cancellation passed. The real flow reached GitHub's browser login page. Input automation was stopped while correcting the test driver's page selection: it must require the exact device-code path and exclude two-factor/passkey pages before pasting a code. The previously verified token session was preserved. Real OAuth completion and a post-feature scan are not claimed. Poco was not advanced to this APK.

The correction is in `phase05-002-browser`; production OAuth code is unchanged apart from version identity. This interruption was a test-harness safeguard correction, not a Play Protect warning. Browser authentication/authorization still requires completion before an OAuth pass can be recorded.
