---
name: android-design
version: "1.0.0"
description: Google's Android design guidance and Material 3 Expressive, with the reasoning beneath them and a method for applying them. Use when designing, reviewing, or building native Android UI in Jetpack Compose or Views for phone, tablet, foldable, desktop windows, Wear OS, TV, cars, XR, or widgets; when porting an iOS or web design to Android; or when checking a screen against Google's quality guidelines. Covers expressive vs restrained design, color and dynamic color, type, shape, motion, Material Symbols, window size classes and adaptive layouts, edge-to-edge, predictive back, navigation, notifications and Live Updates, accessibility, and large-screen quality. Trigger for "follow Material Design", "Material 3", "design an Android screen", "make it feel like a Google app", "port this iOS screen to Android", "is this large-screen ready".
license: MIT (see LICENSE). Android, Material Design and related names are trademarks of Google LLC; independent, unaffiliated reference (see NOTICE).
---

# Android design

Design judgement for **native Android**: Material 3 and Material 3 Expressive, Google's Android design guides, the adaptive behavior the platform now enforces, and Google's quality guidelines. Jetpack Compose comes first (Google's primary toolkit; Views are in maintenance mode but still supported), with notes for Views where they differ. Flutter, React Native and web Material are out of scope as implementation targets; iOS and web designs are covered as *translation* to Android. Play Store submission and policy belong to the sibling Play kit, never to design rules here.

**Current to:** Android 17 (API 37), Compose Material 3 1.4 stable / 1.5 beta, October 2026. Versions and dates live in `references/versions.md`; newer facts go in `.living/KNOWLEDGE.md` when the sidecar is installed.

## How to use this skill

Load only what the task needs: this file plus one or two references is usually enough.

| Task | Read |
|---|---|
| Design a new screen or flow | `references/applying.md` §1–4, then the surface file |
| Review a screen | the checklist below, then `references/quality.md` |
| Port an iOS (or web) design | `references/from-ios.md` |
| Tablets, foldables, desktop windows | `references/adaptive.md` |
| Color, type, shape, motion, icons | `references/foundations.md` |
| How expressive to be | `references/expressive.md` |
| Pick or use a component | `references/components.md` |
| Navigation, settings, sign-in, permissions, notifications, Live Updates, states, copy | `references/patterns.md` |
| Accessibility acceptance criteria | `references/accessibility.md` |
| Make a design implementable (states, events, saved state, offline) | `references/architecture.md` |
| Wear OS · TV · cars · XR and AI glasses · widgets | `references/wear.md` · `tv.md` · `cars.md` · `xr-glasses.md` · `widgets.md` |
| Why Google does it this way | `references/philosophy.md`, `references/psychology.md` |
| Library and OS versions | `references/versions.md` |
| An unfamiliar term | `references/glossary.md` |

## Intake first

Before designing, establish: the person's task; the surface(s); toolkit and library versions; **minSdk, targetSdk and the Android versions in use**; window sizes, postures and inputs (touch, keyboard, mouse, stylus, D-pad, rotary, voice); the people's abilities; brand constraints. Many rules are **target-SDK gated**: name the gate when you state them. `references/applying.md` §1 has the intake table.

## Core principles

These are this skill's synthesis of Google's values (`references/philosophy.md` §3):

1. **Adapt to the window, not the device.** Decide from the window size class, posture and input. (`adaptive.md`)
2. **The person's settings win:** font scale, display size, theme, dynamic color, contrast, animations off. (`accessibility.md`)
3. **Expression is a dial**, set by task criticality, density and brand; accessibility holds at every setting. (`expressive.md`)
4. **One navigation model:** Back is universal and predictive, Up follows the hierarchy, the start destination is fixed. (`patterns.md` §1)
5. **Tone and surfaces carry hierarchy**; shadows and lines are the exception. (`foundations.md`)
6. **Motion explains change and can be turned off.** (`foundations.md` §5)
7. **Glanceable, interruptible, resumable:** state survives rotation, folding, process death. (`architecture.md`)
8. **Quality guidelines are the floor, not the goal.** (`quality.md`)

## Quick rules

Each carries its scope; details and sources are in the named file.

- **Touch targets 48x48dp**, even when the visible element is smaller. Pointer targets may be smaller; Google publishes no pointer minimum. (`accessibility.md`)
- **Contrast 4.5:1** for text, **3:1** for large text (18sp regular / 14sp bold) and for meaningful icons and boundaries; check dynamic schemes too. (`accessibility.md`)
- **Text in sp**, scaling up to **200%**, non-linearly, on Android 14+; no fixed heights on text containers. (`accessibility.md`)
- **Window size classes**, width breakpoints **600 / 840 / 1200 / 1600** dp (compact, medium, expanded, large, extra-large; half-open ranges); height breakpoints 480 / 900 dp. (`adaptive.md`)
- **Navigation:** the adaptive scaffold shows a navigation bar when the width is compact **or** the window has compact height (or a tabletop posture), otherwise a rail; a drawer is always a deliberate choice. (`adaptive.md` §2)
- **Edge-to-edge** is enforced for apps with **targetSdk 35** on Android 15+; with **targetSdk 36** on Android 16+ the opt-out is gone. (`adaptive.md`)
- **Predictive back** is on by default with targetSdk 36 on Android 16+. (`adaptive.md`, `patterns.md`)
- **Orientation and resizability locks are ignored on large screens** for recent targets (target-gated; Google's pages state the boundary differently, so test at 599/600/601dp). (`adaptive.md`)
- **Permissions:** ask when the person starts the feature, say what stays off if they decline, never pressure through settings. (`patterns.md` §4)
- **Every state you draw is a field of one immutable UI state; every control is a named event.** (`architecture.md`)
- **Live Updates** only for ongoing, user-started, time-bound activities; package tracking doesn't qualify. (`patterns.md` §6)

## Review checklist

State the surfaces and the target adaptive tier first. Then check:

**Layout and adaptivity**
- [ ] Works at compact, medium and expanded widths and at compact height; no stretched phone layout.
- [ ] Navigation changes with the window (bar ↔ rail); 3–5 bar destinations.
- [ ] Edge-to-edge with correct insets; nothing under the system bars, cutouts or keyboard.
- [ ] Fold, rotate and resize mid-task: same state, no letterboxing, nothing in the hinge.
- [ ] Keyboard: Tab order, Enter sends, Esc closes temporary UI; hover and right-click on desktop.

**Navigation**
- [ ] Back works on every screen and pops one step (panes, sheets, search, keyboard included); a cancelled back gesture changes nothing.
- [ ] No on-screen back button duplicating system Back; Up only for hierarchy; deep links have a sensible back stack.

**Visual**
- [ ] Color roles, not hard-coded colors; light and dark themes; dynamic color tested with an extreme wallpaper; brand colors keep contrast.
- [ ] Type uses Material roles; hierarchy holds at 200%.
- [ ] Shape and elevation from the token scales; expression level matches the task.
- [ ] Material Symbols, consistent style; directional icons mirror in RTL.

**Accessibility**
- [ ] 48dp targets; contrast passes in both themes.
- [ ] TalkBack: every control named by purpose, related items merged, headings marked, custom actions for gestures.
- [ ] 200% text + largest display size: nothing clipped, main action reachable.
- [ ] Animations off: every change still visible.

**States and content**
- [ ] Loading, empty, error, offline and partial-permission states designed; errors say what to do next.
- [ ] Permissions asked in context with a working "no" path; notification permission after a moment of value.
- [ ] Copy in sentence case; buttons say what they do; room for longer translations.

**Surface-specific**: the checklist in `wear.md`, `tv.md`, `cars.md`, `xr-glasses.md` or `widgets.md`; large-screen tiers in `quality.md` §2.

For deeper problems (wrong task, wrong model, wrong level of expression), use the review lens in `references/applying.md`.

## Verify it

- **Previews:** `@PreviewScreenSizes`, `@PreviewFontScales`, `@PreviewLightDark` and `@PreviewDynamicColors` for size, text-scale and theme claims; `@WearPreviewDevices` on Wear.
- **Compose UI Check** in Android Studio previews for accessibility and adaptive issues.
- **Layout Inspector → Load Overlay** to compare the running build with the mockup.
- **Resizable emulator** to switch between phone, foldable, tablet and desktop sizes.
- **Lint** isn't run by the build: add `./gradlew lint` to CI and keep the accessibility, usability and internationalization checks on.
- **On device:** TalkBack, Switch Access, Accessibility Scanner, keyboard only, animations off.
- **Screenshot tests** with `@PreviewTest` (experimental) catch regressions.

## Resources

- Google's Android UI kit, M3 design kit and surface kits on Figma (links in each surface file and in the README).
- Material Theme Builder for color schemes; Material Symbols.
- Sample apps: Now in Android, and the Compose samples (Reply, Jetchat, Jetcaster).
- For implementation details beyond design, Google's own Android agent skills (github.com/android/skills).
