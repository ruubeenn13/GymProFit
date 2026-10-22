# Adaptive layout, system UI and large screens

**Applies to:** phones, tablets, foldables, desktop windowing and external displays; Compose with `material3-adaptive` and the `navigation-suite` artifact, Views noted where different. Versions: `versions.md`.

The single most important idea on modern Android: **design for the window the app is given, not for the device.** The same app runs in a phone portrait window, a landscape split screen, an unfolded book posture and a freely resized desktop window, and on recent targets the platform no longer lets it refuse.

## Contents

1. Window size classes
2. Choosing navigation
3. Panes and canonical layouts
4. What the platform enforces (the target-SDK matrix)
5. Transitions and state
6. Edge-to-edge, insets and system bars
7. Back and predictive back
8. Foldables and postures
9. Desktop windows, pointer and keyboard
10. Large-screen review checklist
11. Sources

---

## 1. Window size classes

Classes are measured on the **app window** in dp, as half-open ranges. Do not branch on device model, physical screen size or orientation.

| Width class | Range | Typical windows |
|---|---|---|
| Compact | width < 600 | phone portrait |
| Medium | 600 ≤ width < 840 | tablet portrait, unfolded foldable portrait |
| Expanded | 840 ≤ width < 1200 | tablet landscape |
| Large | 1200 ≤ width < 1600 | large tablet, desktop window |
| Extra-large | width ≥ 1600 | desktop, external display |

| Height class | Range |
|---|---|
| Compact | height < 480 (phone landscape) |
| Medium | 480 ≤ height < 900 |
| Expanded | height ≥ 900 |

So the width boundaries are 600 / 840 / 1200 / 1600 dp and the height boundaries are 480 / 900 dp.

- In Compose, read the class from `currentWindowAdaptiveInfo()` (the large and extra-large width classes need the opt-in parameter for them; check the pinned adaptive version, since the release notes deprecate an older overload). Hoist state above the layout switch so nothing is lost when the class changes.
- Use `BoxWithConstraints` (or a custom layout) for component-level decisions inside a pane; use the window class for screen-level decisions.
- The large and extra-large classes are recent; older code that only knows compact/medium/expanded treats every desktop window as "expanded". Decide deliberately what changes above 1200dp (often: a third pane, a wider max content width, or nothing).

## 2. Choosing navigation

Separate two questions: **what Material recommends** and **what the scaffold does by default.**

- **Library default.** `NavigationSuiteScaffold` shows a **navigation bar when the window has compact width or compact height, or the device is in tabletop posture; otherwise a navigation rail.** It never switches to a drawer by itself; a permanent or modal drawer is an explicit override. A medium-width landscape window therefore keeps a bar if its height is compact.
- **Material guidance.** Bar for three to five top-level destinations on compact windows, labels always shown. Rail for wider windows, three to seven destinations, with the FAB (if any) at its top. A modal expanded rail or drawer when destinations are many or include secondary sections.
- **Decide by** number and hierarchy of destinations, window width *and* height, posture, and reach (a rail on a large tablet sits near the thumbs; a bar stretched across 1200dp does not).
- Don't hide navigation on scroll when a screen reader is active. Don't swipe between top-level destinations.

## 3. Panes and canonical layouts

Google's canonical layouts:

- **List-detail.** A list and the selected item. `ListDetailPaneScaffold` shows one pane on compact windows and two when space allows, and handles pane-aware Back.
- **Supporting pane.** Primary content (about two-thirds) plus supporting content. `SupportingPaneScaffold`.
- **Feed.** A grid of equivalent items that reflows by width (lazy grids with adaptive column counts).

Guidance by class, as recommendations rather than rules: compact uses one pane and 16dp margins; medium usually one pane (two are possible), 24dp margins; expanded and wider usually two panes; extra-large can carry three. Margin and pane-width tables on Material's site did not render reliably in research, so treat exact pane widths as the scaffold's defaults rather than as published constants.

Recent adaptive releases add pane expansion (a draggable divider) and pane strategies for Navigation 3; check `versions.md` before relying on them.

## 4. What the platform enforces

Several behaviors depend on **both the Android version the device runs and the targetSdk the app declares.** State the gate whenever you cite one.

| Behavior | Device runs | App targets | Effect |
|---|---|---|---|
| Edge-to-edge | Android 15+ | targetSdk 35 or higher | Content draws behind system bars by default; an opt-out attribute still exists. |
| Edge-to-edge, no opt-out | Android 16+ | targetSdk 36 or higher | The opt-out is ignored. (Targeting 36 but running on 15 still honors it.) |
| Predictive back animations | Android 16+ | targetSdk 36 or higher | System back-to-home, cross-task and cross-activity animations by default; `onBackPressed` and `KEYCODE_BACK` are no longer dispatched. A temporary opt-out exists. Custom in-app transitions are still yours to build. |
| Large-screen resizability | Android 16 | targetSdk 36 | On large screens (smallest width 600dp and up) the system ignores manifest and runtime orientation locks, `resizableActivity="false"` and min/max aspect ratios. A temporary per-app or per-activity opt-out exists. Games (by app category) are exempt; users can still choose an aspect ratio in settings. |
| Large-screen resizability, no opt-out | Android 17 | targetSdk 37 | Same, without the opt-out. |

**Boundary conflict.** Google's Android 17 page describes the rule as applying to displays **>600dp**, while the Android 16 page describes it as smallest width **at least sw600dp**. Treat this as an open conflict: design so the app works unlocked at any size, and test at 599, 600 and 601dp. Note that this is the **physical display's smallest width**, which is a different measurement from the app window's size class.

**What it means for design.** "Portrait only" is no longer a design option on large screens. Every screen needs a layout that works in landscape and in arbitrary aspect ratios, without letterboxing, and without losing state when the window changes. Android 17 also lists the classic View UI components and View-based Jetpack libraries as in maintenance mode: existing View apps keep working, but new adaptive features arrive in Compose first.

## 5. Transitions and state

Breakpoints are the easy part; transitions are where apps break. Design and test:

- **Continuous resize** (desktop windows, split-screen dividers): no flicker of the wrong layout, no reset of scroll position.
- **Fold and unfold:** the same item stays selected and visible; in list-detail, folding to compact shows the pane the person was using.
- **Compact height:** landscape phone or a short split-screen window; bottom sheets, dialogs and the keyboard compete for very little height.
- **IME visible:** the focused field and its confirming action stay on screen.
- **Process death:** selection, scroll, drafts and in-progress input restore after the system kills the app.
- **Focus transfer:** when panes appear or disappear, keyboard and screen-reader focus land somewhere sensible, not at the top of the screen.
- **External display:** density can change when a window moves between screens; images and touch targets must re-resolve.

## 6. Edge-to-edge, insets and system bars

- Draw backgrounds and scrolling content under the status bar and navigation bar; keep text and tap targets out of the system bar, gesture and display-cutout insets. Dividers and backgrounds can run full-bleed; content and controls get padding.
- Inset types to handle: system bars, system gestures (don't put drag targets at the screen edges where back swipes start), display cutout, and the IME. Desktop windows add caption bars.
- Status bar: transparent or translucent while content scrolls beneath it. Use the top app bar's own scroll treatment or one custom protection gradient, never both.
- Gesture navigation: keep the bar area transparent. Three-button navigation: the system adds a translucent scrim; remove it only when your own bottom bar sits there.
- Each pane and each drawer manages its own inset protection.
- Immersive content (video, games, reading) may hide system bars but must let a tap or swipe bring them back.

## 7. Back and predictive back

- **Back** follows history (what the person did); **Up** follows the app's hierarchy and never leaves the app. On Android, Up is the arrow in the top app bar; Back is the system gesture or button. Do not draw a custom on-screen back button that duplicates system Back.
- **Predictive back** shows a preview as the gesture progresses, and a cancelled gesture must return exactly to where it was. Material components animate their own predictive back (sheets, search, drawers, the modal expanded rail).
- Design values for a full-screen custom transition: the exiting surface scales from 100% to 90%, the entering one from 110% to 100%, with a fade-through at 35% of progress. These are design targets for custom transitions, not a reason to hand-scale every screen; prefer the components' built-in behavior.
- In multi-pane layouts, Back should collapse to the previous pane state on compact windows and should not "close" a pane that remains visible on wide ones. The adaptive scaffolds do this when navigation is wired through them.

## 8. Foldables and postures

- **Book posture** (vertical fold, half-open): a natural two-pane split along the hinge.
- **Tabletop posture** (horizontal fold, half-open): content on the top half, controls on the bottom half; the navigation scaffold switches to a bar in tabletop by default.
- **Hinges:** a separating hinge (or one that occludes pixels) must not have text, faces or controls straddling it. Read the fold's bounds from the window layout info; do not hard-code device geometry.
- Folding is a configuration change: same features, same state, both screens.

## 9. Desktop windows, pointer and keyboard

- Hover states on everything interactive; right-click opens a context menu; drag starts immediately with a pointer; selection checkboxes can appear on hover.
- Pointer targets may be smaller than touch targets, but if touch is also available keep 48dp. There is no single published pointer minimum; match the visible target to the hit area.
- Full keyboard navigation and shortcuts for frequent actions; Tab order follows reading order; visible focus.
- Constrain line length and button width on wide windows rather than stretching.
- Multi-instance (two windows of the app), drag and drop between apps, and picture-in-picture are part of the higher adaptive quality tiers (`quality.md`).

## 10. Large-screen review checklist

- [ ] Layout decided from window classes (width *and* height), never device model or orientation.
- [ ] Works unlocked in every orientation and aspect ratio on large screens; tested at 599/600/601dp smallest width.
- [ ] Navigation follows §2; bar returns at compact height.
- [ ] Panes follow a canonical layout where one fits; Back is pane-aware.
- [ ] State survives rotation, fold/unfold, resize, split screen and process death.
- [ ] Edge-to-edge with correct insets, including IME and cutouts; no controls in gesture areas.
- [ ] Predictive back previews correctly; a cancelled gesture restores the screen.
- [ ] Hinge-aware on foldables; tabletop splits content and controls.
- [ ] Pointer hover, right-click, keyboard navigation and shortcuts on desktop windows.
- [ ] Quality tier target named and checked (`quality.md`).

## 11. Sources

- Window size classes: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Adaptive navigation: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Support different display sizes: https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes
- Canonical layouts: https://m3.material.io/foundations/layout/canonical-layouts/overview
- material3-adaptive releases: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- Behavior changes, Android 16: https://developer.android.com/about/versions/16/behavior-changes-16
- Restrictions ignored, Android 17: https://developer.android.com/about/versions/17/changes/ff-restrictions-ignored
- Android 17 announcement: https://android-developers.googleblog.com/2026/06/Android-17.html
- Edge-to-edge design: https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge
- System bars: https://developer.android.com/design/ui/mobile/guides/foundations/system-bars
- Insets in Compose: https://developer.android.com/develop/ui/compose/system/insets
- Predictive back design: https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
- Navigation principles: https://developer.android.com/guide/navigation/principles
- Desktop pointer interactions: https://developer.android.com/design/ui/desktop/guides/interaction/pointer-interactions
- Adaptive app quality: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
