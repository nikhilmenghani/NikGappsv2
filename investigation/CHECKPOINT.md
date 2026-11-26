# Phase 02.003 recreation — Startup restoration

A normal rotation test on Poco reproduced a startup regression in the staged port: Activity recreation could wait indefinitely for a system splash callback that does not run on rotation. Restored Activities now proceed directly to their saved UI, and native splash removal is scheduled on the attached window decor.

The dynamic-color capability is otherwise identical to phase02-003. Production-normalized signing. Device checks include cold start, rotation, restored navigation, theme controls and Play Protect.
