# phase03-006 — outdated-project deck, swipes and rebound

Ports the original stacked-card layout and gesture animation. Outdated cards interpolate from the deck during a downward drag, collapse with an upward drag across the expanded group, and settle with a velocity-dependent spring. Regular project cards share the rebound; the app header stays fixed. A tap uses a smaller regular rebound. Deck clipping prevents stray upper corners.

No new production permissions or dependencies. The separate temporary instrumentation APK renders the actual component with short and long in-memory groups, without modifying saved projects. Registry integration remains a later checkpoint.

Verification: release lint, 11 JVM tests and component-test compilation. Device checks will cover slow/fast gestures, tap expansion, reversal, anchored long-list layout and light/dark/classic styling, then removal of the test tool and manual scans on OnePlus followed by Poco. No device pass is claimed in advance.
