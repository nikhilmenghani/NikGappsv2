# NikGappsv2

A staged port of NikGapps and a controlled Google Play Protect investigation.

The empty baseline and ordinary/normalized signing controls passed immediate installation and Play Protect checks on both test phones. The phased port now includes branding, splash, navigation, appearance, local projects and pinned registry components. See [the evidence ledger](investigation/RESULTS.md) and [feature inventory](FEATURES.md) for progress. Account UI, package-selection integration, file access, ZIP building, background work, updates and analytics have separate later checkpoints and are not yet included.

Each capability receives a commit and immutable GitHub prerelease. Download the exact released APK, verify its hash, install through Android on OnePlus 7 Pro, exercise the feature and complete a manual Play Protect scan. Test the same bytes on Poco X3 Pro only after OnePlus passes. Record immediate observations with UTC timestamps. Stop cumulative porting on a harmful warning and perform removal/reintroduction controls, then confirm any fix in the original package.

Package ID is `com.nikgapps.v2`. The release signing certificate is the existing NikGapps certificate. Releases are experimental and install alongside NikGapps.
