# NikGappsv2

A staged port of NikGapps and a controlled Google Play Protect investigation.

The initial release contains one framework Activity. The baseline declares no permissions and includes only AGP's automatic Kotlin standard library (2.2.10) and annotations (13.0) as runtime dependencies. Branding, Compose, networking, account access and background work are separate future checkpoints.

Each capability receives a commit and immutable GitHub prerelease. Download the exact released APK, verify its hash, install through Android on OnePlus 7 Pro, exercise the feature and complete a manual Play Protect scan. Test the same bytes on Poco X3 Pro only after OnePlus passes. Record immediate observations with UTC timestamps. Stop cumulative porting on a harmful warning and perform removal/reintroduction controls, then confirm any fix in the original package.

Package ID is `com.nikgapps.v2`. The release signing certificate is the existing NikGapps certificate. Releases are experimental and install alongside NikGapps.
