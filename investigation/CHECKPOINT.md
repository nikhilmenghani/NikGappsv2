# Phase 03.003 - Edit, copy and delete projects

Adds the themed list action menu for tapping or holding a project, reuses the top sheet for editing, copies with a new identity/date, and confirms deletion with the shared divider and equal-width buttons. Ripple clipping follows the card's shape. Radial menus and package-selection navigation are separate later checkpoints.

Edits preserve project identity and creation time; copies preserve the target and obey the regular five-project limit. Missing projects cannot be resurrected by an edit. Release tests cover these mutation and persistence rules.

Verify long press, edit/save/cancel, copy, delete/cancel/confirm, restart persistence and themed menus/dialogs, then run the normal installer and manual Play Protect checks on OnePlus and Poco. Results follow in the [ledger](https://github.com/nikhilmenghani/NikGappsv2/blob/main/investigation/RESULTS.md).
