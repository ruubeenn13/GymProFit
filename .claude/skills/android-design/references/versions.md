# Versions matrix

**Applies to:** every reference in this skill. Checked 2026-10-18. Newer facts live in `.living/KNOWLEDGE.md`; when it disagrees with this table, it wins for dates and versions only.

## Platform

| Item | Status | Date | Source |
|---|---|---|---|
| Android 17 (API 37) | stable | June 16, 2026 | https://android-developers.googleblog.com/2026/06/Android-17.html |
| Android 17 QPR2 (SDK 37.2) | beta, latest Beta 7.1 | October 16, 2026 | https://developer.android.com/about/versions/17/qpr2/release-notes |
| Android 16 (API 36) | stable | June 10, 2025 | https://android-developers.googleblog.com/2025/06/android-16-is-here.html |
| Material 3 Expressive | announced | May 13, 2025 | https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/ |

Android 17 states that Compose is the primary UI toolkit and puts View components and View-based Jetpack libraries in maintenance mode. Existing View apps keep working.

## Target-SDK gates (see `adaptive.md` §4)

| Behavior | Runtime | targetSdk |
|---|---|---|
| Edge-to-edge enforced (opt-out available) | Android 15+ | 35+ |
| Edge-to-edge opt-out removed | Android 16+ | 36+ |
| Predictive back animations by default | Android 16+ | 36+ |
| Large-screen orientation/resizability locks ignored (temporary opt-out) | Android 16 | 36 |
| Same, no opt-out | Android 17 | 37 |

## Libraries

| Artifact | Stable | Preview | Source |
|---|---|---|---|
| Compose Material 3 | 1.4.0 (September 24, 2025) | 1.5.0-beta01 (October 7, 2026); most Expressive APIs | https://developer.android.com/jetpack/androidx/releases/compose-material3 |
| Compose Material 3 Adaptive | 1.3.0 (August 12, 2026) | 1.4.0-alpha03 (October 7, 2026) | https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive |
| MDC-Android (Views) | 1.14.0 (May 13, 2026), first with Expressive | | https://github.com/material-components/material-components-android/releases/tag/1.14.0 |
| Wear Compose Material 3 | 1.5.0 first stable (August 27, 2025) | | https://developer.android.com/jetpack/androidx/releases/wear-compose-m3 |

## Tooling and surface libraries

| Item | Version facts | Source |
|---|---|---|
| Compose multipreview (`@PreviewScreenSizes`, `@PreviewFontScales`, `@PreviewLightDark`, `@PreviewDynamicColors`) | ui-tooling-preview 1.6.0-alpha01+ | https://developer.android.com/develop/ui/compose/tooling/previews |
| Preview screenshot testing (`@PreviewTest`) | plugin 0.0.1-alpha16, experimental; standalone plugin deprecated from AGP 9.5.0-alpha03 | https://developer.android.com/studio/preview/compose-screenshot-testing |
| Live Edit | Compose Runtime 1.3.0+, device API 30+ | https://developer.android.com/develop/ui/compose/tooling/iterative-development |
| Photo picker (`PickVisualMedia`) | androidx.activity 1.7.0+ | https://developer.android.com/training/data-storage/shared/photopicker |
| Full-bleed widgets on Android 17 | Glance 1.3.0-alpha02+, compileSdk 37, AGP 9.1.0+ | https://developer.android.com/design/ui/mobile/guides/widgets/layouts |
| Car light and dark themes | Car App Library 1.9+ | https://developer.android.com/docs/quality-guidelines/car-app-quality |

## Upcoming

- Android Dev Summit: October 28, 2026 (https://developer.android.com/events/dev-summit). Refresh this matrix afterwards and when Compose Material 3 1.5.0 goes stable.

## Sources

The URLs in each table row, plus the Android versions index: https://developer.android.com/about/versions
