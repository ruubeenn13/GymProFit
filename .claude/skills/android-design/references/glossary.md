# Glossary

Android and Material terms used in this skill, paraphrased from Google's glossary and guides. "(iOS: …)" marks a term that iOS vocabulary gets wrong.

- **Activity** — one screen's entry point that the system can start on its own; most Compose apps use a single activity. (iOS: closest to a scene or root view controller.) · `adaptive.md`
- **Canonical layout** — Google's predefined large-screen layouts: list-detail, feed, supporting pane. (iOS: no equivalent; NavigationSplitView is the closest.) · `adaptive.md`
- **Chroma** — the HCT axis for colorfulness, from 0 (gray) to about 120. · `foundations.md`
- **Containment** — visual grouping (card, sheet, dialog) that shows what belongs together; implicit containment uses space and alignment instead. · `components.md`
- **CTA** — call to action: the control that moves the person to the next step.
- **Display cutout** — the area a camera or sensor takes out of the screen; content handles it through insets. · `adaptive.md`
- **dp** — density-independent pixel; about 1 px at 160 dpi. (iOS: pt.) · `foundations.md`
- **Edge-to-edge** — the app draws behind the system bars and pads its content with insets. · `adaptive.md`
- **Hinge gutter** — the separating region on a foldable's hinge; layouts keep content out of it. · `adaptive.md`
- **HCT** — hue, chroma, tone: Material's color space; tone differences drive contrast. · `foundations.md`
- **Hue** — the HCT axis for color family, 0–360. · `foundations.md`
- **Insets** — the sizes of system bars, cutouts and the keyboard, which content must avoid or pad for. · `adaptive.md`
- **Intent** — a message that asks another component or app to do something (open a link, take a photo). (iOS: URL schemes and extensions, roughly.) · `patterns.md`
- **Navigation bar (system)** — the bottom system bar for Back, Home and Recents (gestures or three buttons). Not the app's navigation bar component, and not the iOS "navigation bar" (a top bar). · `adaptive.md`
- **Navigation bar (component)** — the app's bottom bar for 3–5 top-level destinations. (iOS: tab bar.) · `components.md`
- **One-time permission** — "Only this time": a permission granted for the current session (location, camera, microphone; Android 11+). · `patterns.md`
- **Pane** — one region of an adaptive layout (list, detail, supporting); the number of panes follows the window class. · `adaptive.md`
- **Posture** — a foldable's physical state: flat, half-open, tabletop (hinge horizontal) or book (hinge vertical). · `adaptive.md`
- **Predictive back** — the back gesture previews its destination before the person commits. · `adaptive.md`
- **Privacy Dashboard** — the Android 12+ settings page showing when apps used location, camera and microphone. · `patterns.md`
- **sp** — scale-independent pixel: dp that also follows the person's font size; for text only. (iOS: Dynamic Type sizes.) · `foundations.md`
- **SSOT** — single source of truth: one owner per type of data. · `architecture.md`
- **State holder** — the object that owns a screen's state (a ViewModel) or a component's UI state (a `remember`ed class). · `architecture.md`
- **Status bar** — the top system bar with the time, battery and notification icons. · `adaptive.md`
- **Task and back stack** — the activities and destinations the person has opened, in order; Back pops them. (iOS: navigation stack.) · `patterns.md`
- **Tier 3 / 2 / 1** — Google's quality tiers, from the required minimum (3) to differentiated (1). · `quality.md`
- **Tone** — the HCT axis for lightness, 0 (black) to 100 (white). · `foundations.md`
- **TTID / TTFD** — time to initial display (first frame) and time to full display (usable). · `quality.md`
- **UDF** — unidirectional data flow: state flows down, events flow up. · `architecture.md`
- **Window size class** — the window's width bucket (compact, medium, expanded, large, extra-large) and height bucket (compact, medium, expanded). · `adaptive.md`

Source: https://developer.android.com/design/ui/mobile/guides/foundations/glossary (checked 2026-10-18) and the guides cited in each file.
