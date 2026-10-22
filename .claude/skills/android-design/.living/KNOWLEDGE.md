---
last_updated: 2026-10-18
review_interval_days: 30
mode: advisory
spec: living-skills/1.1
---

# Android design — current facts

> Baseline note (2026-10-18): SKILL.md v1.0.0 and its references were written
> on this date and already contain everything below. From here on, this file
> only needs to carry what changes after 2026-10-18.

## Platform

- Android 17 (API 37) stable June 16, 2026. Android 17 QPR2 in beta (Beta 7.1,
  October 16, 2026, SDK 37.2).
- Compose is Google's primary UI toolkit; View components are in maintenance mode.
- Target-SDK gates: edge-to-edge enforced at targetSdk 35 (Android 15+), opt-out
  removed at 36 (Android 16+); predictive back default at 36; large-screen
  orientation/resizability locks ignored at 36 on Android 16 (temporary opt-out)
  and 37 on Android 17 (no opt-out). Boundary stated as ">600dp" vs "sw600dp":
  unresolved conflict.

## Libraries

- Compose Material 3 1.4.0 stable (September 24, 2025); 1.5.0-beta01 (October 7, 2026).
- material3-adaptive 1.3.0 (August 12, 2026).
- MDC-Android 1.14.0 (May 13, 2026), Expressive for Views.
- Wear Compose Material 3 1.5.0 first stable (August 27, 2025).

## Watch next

- Android Dev Summit, October 28, 2026.
- Compose Material 3 1.5.0 stable; Navigation 3 version and API names.
- Android vitals render, startup and wake-up thresholds (not yet verified).
- TV type sizes and the TV safe-margin conflict.
