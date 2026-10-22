# From iOS (and other platforms) to Android

**Applies to:** porting an iOS/iPadOS design to Android phones and large screens; the same procedure works for a web design. Conceptual translation only: implementation is native Android (Compose or Views). Pair with the `apple-hig` skill when a product ships on both.

Google publishes its own translation guide (Sources). Its order is roughly: drop iOS system bars, resize the frame to an Android size, replace bars and navigation with Android's, then controls, then style, then tidy up. The table below goes further: each row is a decision with conditions and a test, not a one-to-one swap. A row that says "→ X" without a condition is a trap.

## 1. Procedure

1. **Start from intent.** For each iOS element, write what the person is trying to do with it.
2. **Frame.** Redesign at an Android compact width (Google suggests a 412dp-wide frame and checking at 360dp), then at medium and expanded widths (`adaptive.md`). The Android version is one adaptive design, not a phone screen plus a stretched tablet screen.
3. **Replace system chrome.** Remove the iOS status bar, home indicator and navigation bar; apply Android's edge-to-edge system bars and insets.
4. **Navigation and Back.** Apply rows 1–6 below. Android has a system Back on every screen; nothing in the design should depend on a visible back button.
5. **Controls and patterns.** Apply the remaining rows.
6. **Style.** Map type to Material roles, colors to roles (and decide on dynamic color), icons to Material Symbols, shapes to the corner scale.
7. **Test** with the checklist in `SKILL.md` and the scenarios in §3.

## 2. Decision table

| # | iOS element | Person's intent | Android choice | Conditions and exceptions | Test |
|---|---|---|---|---|---|
| 1 | Tab bar | switch between top-level areas | Navigation bar on compact; rail on wider windows (navigation suite) | 3–5 destinations. More than five: restructure, or use an expanded rail/drawer on large windows; no "More" tab. The scaffold keeps a bar at compact height. | resize through 600/840dp and a landscape phone |
| 2 | Sidebar (iPad) | browse a hierarchy of sections | Expanded navigation rail, or a standard drawer for deep hierarchies | Choose by destination count and depth, window width *and* height, and reach. The scaffold won't pick a drawer for you. | large window: every section reachable in one tap |
| 3 | Navigation bar with title | know where I am; act on this screen | Top app bar: small, or medium/large flexible for a large title | Medium/large collapse on scroll. One primary action, about two trailing icons. | scroll a long screen; title remains legible |
| 4 | Back button with previous title | go back where I came from | System Back (gesture/button); the Up arrow in the top app bar for hierarchy | Back follows history, Up follows hierarchy and never leaves the app. Deep links need a synthetic back stack. Pane collapse and keyboard dismissal are Back steps too. Cancelled predictive gestures return to the same state. | deep-link in, press Back; cancel a back gesture halfway |
| 5 | Swipe from left edge to go back | go back | System Back gesture | Don't put drag targets at the screen edges. | swipe from both edges in a carousel |
| 6 | Modal stack (sheet over sheet) | finish a sub-task without losing context | Full-screen dialog on compact windows; dialog or side sheet on larger ones | Decide from task size and available space. Keep "discard changes?" semantics; Back = cancel. | rotate mid-edit; Back with unsaved changes |
| 7 | Primary action button | do the main thing | A FAB only for the screen's main creating/starting action; otherwise an inline filled button | Form submission and confirmation are inline buttons, never FABs. One FAB per screen. | is the FAB the most common action on this screen? |
| 8 | Bottom toolbar | act on the current content | Toolbar (docked or floating) | Not together with a navigation bar on the same screen. | thumb reach on compact; placement on expanded |
| 9 | Segmented control | switch views of sibling content | Tabs | When it chooses between sibling views of the same content. | swipe between tabs works |
| 10 | Segmented control | pick an option or filter | Connected button group, or filter chips | When it selects a value. Record single vs multiple selection and whether the choice persists. | the selection survives rotation |
| 11 | Action sheet | choose one of several actions | Modal bottom sheet or menu | Menu when anchored to a control; bottom sheet for richer choices; destructive actions clearly marked. | dismiss by Back and by tapping outside |
| 12 | Alert | make a critical decision now | Dialog | Up to two actions; confirming action trailing; never disable dismiss. Not for routine information (use a snackbar or inline message). | screen reader reads title, text and both actions |
| 13 | Context menu (long press) | act on this item | Menu (long press or right-click) | Also expose the actions as accessibility actions. | right-click on desktop; TalkBack actions |
| 14 | Pull-down menu / popover | choose from a short list | Menu, or exposed dropdown for a field value | | keyboard navigation through the menu |
| 15 | Toggle | turn a setting on/off now | Switch | Checkbox when the choice is confirmed later or is one of many in a list. | announces on/off |
| 16 | Table view cell | scan and open items | List item | Leading/trailing elements; one to three lines. | 200% text: rows grow, nothing clips |
| 17 | Collection view | browse visual items | Lazy grid (adaptive columns) or carousel | Carousel for browsing, not for critical info. | column count changes with width |
| 18 | Search bar in navigation bar | find content | Search bar expanding to a search view | Inline on large windows where search is central. | results update as you type |
| 19 | Large Title … Caption text styles | read with hierarchy | Material type roles (display, headline, title, body, label) | Map by role, not by point size; emphasized roles for the few hero texts. | hierarchy holds at 200% |
| 20 | Dynamic Type | read comfortably | Font scale up to 200%, non-linear, text in sp | No fixed heights on text containers. | 200% + largest display size |
| 21 | SF Symbols | recognize actions | Material Symbols | Filled for selected, outlined otherwise; standard symbol for standard actions. Mirror directional icons in RTL. | names announced; RTL arrows flip |
| 22 | 44x44pt minimum target | hit things reliably | 48x48dp touch target | Pointer targets may be smaller where touch isn't used. | Accessibility Scanner target check |
| 23 | Size classes (compact/regular) | fit the space | Window size classes: five widths, three heights | Half-open ranges; decide from the window, not the device. | 599/600/601dp |
| 24 | Liquid Glass and materials | separate controls from content | Tonal surface containers, elevation by tone, scrims | Android does use transparency for system-bar protection and scrims; don't invent glass for app chrome, but don't ban transparency either. | contrast of controls over content in both themes |
| 25 | Accent and brand color | recognize the brand; spot actions | Color roles, optional dynamic color | Brand primary + dynamic neutrals is a common middle path; semantic colors fixed. | dynamic scheme from an extreme wallpaper |
| 26 | App icon (layered) | recognize the app | Adaptive icon (108dp layers, 66dp safe zone) + monochrome layer | Plus the Play Store 512px icon. | themed icons on; circle and squircle masks |
| 27 | Live Activities / Dynamic Island | follow an ongoing activity | Live Update (Android 16+) | Only if eligible and time-bound (not package tracking); the system or person may decline promotion, so an ordinary ongoing notification is the fallback. | promotion off: still useful |
| 28 | Widgets (WidgetKit) | glance at info on the home screen | App widgets (Glance is one way to build them) | Host decides the size; responsive content; not feature-equivalent to WidgetKit. | smallest and largest size, dark theme |
| 29 | Haptics (feedback generators) | feel confirmation | Predefined haptic feedback constants | Crisp, purposeful, respecting settings. | haptics off in settings |
| 30 | Reduce Motion | avoid motion | Remove animations setting | Compose `MotionDurationScale`; Views `areAnimatorsEnabled()`; test custom animations. | animations off: no lost information |
| 31 | VoiceOver | use without sight | TalkBack | Merged semantics, roles, headings, custom actions. | full task with TalkBack |
| 32 | Foldable iPhone / iPad multitasking | keep working as the space changes | Foldables, split screen, desktop windows | Same features and state at every size; postures; hinges. | fold/unfold mid-task |
| 33 | App Review guidelines | get accepted in the store | Play policy (Play kit) **and** separately Google's quality guidelines (`quality.md`) | Different things: store policy vs technical quality. | |
| 34 | Push notification design | be told what matters | Notification channels, styles, permission in context | Android 13+ permission; channels let people tune categories. | permission denied path |

## 3. Scenarios to check after porting

- An iOS app with **seven tabs**: restructure to five or fewer on compact, or move to an expanded rail on large windows; never a "More" tab.
- An iOS **page sheet** with a long form on a landscape phone: compact height leaves little room; a full-screen dialog fits, a bottom sheet doesn't.
- An iOS app that relies on a **back button label** for orientation: the Android version needs a clear title in the top app bar because Back shows no label.
- An iOS **portrait-only** app: on large Android screens the lock is ignored for recent targets; design landscape.
- **Live Activity for parcel tracking:** not eligible as a Live Update; use an ordinary notification.

## 4. Sources

- Translate designs to Android: https://developer.android.com/design/ui/mobile/guides/foundations/translate-designs
- Adaptive navigation: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Window size classes: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Navigation principles: https://developer.android.com/guide/navigation/principles
- Live Updates: https://developer.android.com/develop/ui/views/notifications/live-update
- Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
