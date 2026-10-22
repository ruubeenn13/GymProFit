# Wear OS

**Applies to:** Wear OS apps, tiles and complications built with Wear Compose Material 3 (first stable 1.5.0, August 27, 2025) on Wear OS 6 and later. Watch faces follow their own Watch Face Format criteria, noted where they differ.

A watch is glanced at for a few seconds, on a small round screen, often with one hand busy. Every rule below comes from that: fewer things on screen, bigger targets, one task at a time, and black around everything.

## Contents

1. Surfaces: which one to build
2. Screen sizes and layout
3. Color, type and shape
4. Interaction
5. Quality checklist
6. Sources

---

## 1. Surfaces: which one to build

| Surface | Use it for | Shape of the content |
|---|---|---|
| **Tile** | the one thing a person wants without opening the app (next run, timer start, today's steps) | non-scrolling, template-based; purpose obvious at a glance; only the most critical information |
| **Complication** | a single value on the watch face | a number, short text, icon or range; data, not UI |
| **App** | tasks that need more than a glance (a workout, a reply) | scrolling lists (`TransformingLazyColumn` / `ScalingLazyColumn`) or a non-scrolling screen (media player, picker, progress) |
| **Ongoing activity / Live Update** | something running that the person wants back quickly (a workout, navigation) | an icon on the watch face and in Recents; bridges the phone's Live Update |
| **Notification** | bridged from the phone, or watch-specific | short; actions the person can do on the wrist |

Google's tile principles: **immediate, predictable, relevant**. If a tile needs scrolling, it is an app screen.

## 2. Screen sizes and layout

- Round screens from **192dp**. Google splits them at a **225dp** breakpoint: small (192–224dp) and large (225dp and up). Design at both, plus the largest current device.
- **Margins are percentages**, not fixed dp, so the layout survives the curve and different sizes.
- Larger screens must never show *less* information than smaller ones. Wear's own quality tiers are: ready for all screen sizes → responsive and optimized → adaptive and differentiated.
- App layouts use three slots: **top** (time text, title), **middle** (content), **bottom** (an edge-hugging button). A scroll indicator follows the curve.
- Lists: one item per row, full-width chips or cards, scrolling that scales items at the edges. Rotary input (the crown) must scroll every scrollable screen.

## 3. Color, type and shape

- **Dark only, black background.** Black saves power on OLED and blends with the bezel. Wear quality requires a black background for apps.
- **Color:** the watch face's seeds drive primary and tertiary colors; M3 Expressive on Wear brings larger tonal palettes and a third accent.
- **Type:** minimum **12sp** for essential text and **10sp** for non-essential text (quality guideline). Variable fonts with weight and width axes; Arc Text follows the round edge; numerals have their own roles. Support font scaling: Wear previews include font-scale variants (`@WearPreviewFontScales`).
- **Shape:** edge-hugging containers fit the round display; buttons and list items morph shape on press (Expressive).

## 4. Interaction

- **Touch targets 48x48dp** (quality ID WO-V2).
- **Swipe to dismiss** goes back (WO-V3); don't put horizontal swipes in the way of it.
- One primary action per screen; confirmations are a brief animation, not a dialog, unless the action is destructive.
- Short text input: voice, suggested replies and emoji first; the keyboard is a last resort.
- Respect the always-on state in apps that stay visible (an ongoing workout): dim, remove motion, show only what matters.

## 5. Quality checklist

Wear app quality items most often missed (IDs from Google's checklist):

- WO-V1 / WO-V14 text sizes (12sp essential, 10sp non-essential).
- WO-V2 48x48dp targets.
- WO-V3 swipe to dismiss.
- WO-V4 an ongoing activity for ongoing tasks.
- WO-V13 black background.
- WO-V15 splash icon 48dp on black.
- WO-V16 works on a 192dp round screen.
- Watch faces only: always-on (AOD) mode lights no more than **15%** of pixels (a brightness-weighted criterion for watch faces, not a tile rule); at most eight complication slots.

Test on the smallest round emulator, a large one, with font scale at its largest, and with the crown.

## 6. Sources

- Wear OS design: https://developer.android.com/design/ui/wear
- Screen sizes: https://developer.android.com/design/ui/wear/guides/foundations/screen-sizes
- Adaptive layouts: https://developer.android.com/design/ui/wear/guides/foundations/adaptive-layouts
- Common layouts: https://developer.android.com/design/ui/wear/guides/foundations/common-layouts
- Tiles: https://developer.android.com/design/ui/wear/guides/surfaces/tiles
- App layouts: https://developer.android.com/design/ui/wear/guides/surfaces/apps/layouts
- Color system: https://developer.android.com/design/ui/wear/guides/styles/color/system
- Wear app quality: https://developer.android.com/docs/quality-guidelines/wear-app-quality
- M3 Expressive for Wear OS: https://android-developers.googleblog.com/2025/08/introducing-material-3-expressive-for-wear-os.html
- Wear Compose Material 3 releases: https://developer.android.com/jetpack/androidx/releases/wear-compose-m3
