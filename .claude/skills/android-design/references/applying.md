# Applying the guidance: a method

**Applies to:** any Android design or review task, on any surface. Load this first when the task is "design something", "port something", or "this guidance doesn't cover my case".

Google's guidance is a set of answers to questions Google already asked. This file is the method for asking your own. It is the skill's synthesis, not a Google procedure: where a step leans on a Google rule, the rule is named and its source file given, so you can tell a mandate from a judgement.

## Contents

1. Intake: what you must know before you design
2. Constraints: separate mandates from recommendations from taste
3. Alternatives: generate at least two
4. Decision: choose, and write down why the others lost
5. Failure states: design the bad paths
6. Validation: prove it on the device, not in the mockup
7. Worked example A: an iOS tab app becomes an adaptive Android app
8. Worked example B: an inbox across compact, medium, expanded and a fold
9. Worked example C: a Wear OS Tile for the same product
10. The deeper review lens
11. Sources

---

## 1. Intake

Answer these before drawing anything. Most bad Android designs fail on a question nobody asked.

| Question | Why it changes the design |
|---|---|
| **What is the task, in one sentence, for whom?** | Sets what is primary on the screen and how much expression the task can carry. |
| **Which surfaces?** Phone only, or tablet, foldable, desktop windows, Wear OS, TV, car, XR, widget? | Each surface has its own reference file and its own quality checklist. "Phone only" is rarely true: on large screens Android ignores orientation and resizability locks for apps that target recent SDKs (see `adaptive.md` §4). |
| **Toolkit and library versions.** Compose (which Material 3 and adaptive versions) or Views (which MDC version)? | Expressive components and some adaptive scaffolds exist only in certain versions. Check `versions.md`. Do not design for an API the project cannot ship. |
| **minSdk, targetSdk, and the runtime range in the field.** | Several platform behaviors are **target-SDK gated**: edge-to-edge, predictive back, large-screen resizability. The same app behaves differently on Android 15, 16 and 17 depending on what it targets. Name the gate in the design. |
| **Window, posture and input.** Smallest and largest window; compact height (landscape phone, split screen); folded/unfolded/tabletop; touch, mouse, keyboard, stylus, D-pad, rotary. | Navigation and layout depend on width *and* height, and on input. |
| **The people.** Abilities (low vision, motor, cognitive, screen reader users), languages (long translations, right-to-left), context (one-handed, driving, glanceable). | These are acceptance criteria, not polish (`accessibility.md`). |
| **Brand constraints.** Fixed brand colors and typefaces? Will the app follow the user's wallpaper colors? | Dynamic color is opt-in and brand is legitimate (`foundations.md` §2). Decide early which colors are brand, which follow the user, and which are semantic and fixed. |
| **Distribution requirements.** Play quality expectations, large-screen tiers, category rules (cars, kids). | Quality guidelines are a floor (`quality.md`). Store policy belongs to the Play kit, not to design. |

If an answer is unknown, say what you assumed and design so the assumption is cheap to change.

## 2. Constraints

Sort every rule you are about to apply into one of five kinds. The kind tells you how hard it is.

1. **OS behavior.** What the platform does whether you like it or not (edge-to-edge enforcement, ignored orientation locks, predictive back animations, font scaling up to 200%). Design for it; you cannot opt out.
2. **Quality guideline.** What Google checks and publishes as a checklist (core app quality, adaptive tiers, Wear, TV, car, widget quality). Treat MUST items as requirements and SHOULD items as strong defaults.
3. **Material guideline.** What Material recommends (component choice, spacing, motion). Default to it; depart only with a reason you can write down.
4. **Library default.** What a component does out of the box (a scaffold's navigation choice, a token's value). Know it, because it is what ships if you do nothing; it is not the same as the guideline.
5. **Synthesis.** Reasoning this skill adds (the expression dial, the intake). Useful, never binding.

When two kinds disagree, the higher one wins (OS behavior beats a guideline; a guideline beats a library default). When two Google pages disagree with each other, say so, follow the more specific and more recent page, and test both readings.

## 3. Alternatives

Generate at least two genuinely different answers before choosing. Useful axes:

- **Navigation model:** bar vs rail vs drawer vs tabs within a destination vs a single screen with search.
- **Layout:** one pane vs list-detail vs supporting pane vs feed; what happens when the window grows.
- **Expression level:** restrained (utility, dense data, high-stakes tasks) vs expressive (a few hero moments, onboarding, celebration) — see `expressive.md` §4.
- **Interaction:** direct manipulation vs a form; a dialog vs a full-screen task vs inline editing.
- **Surface split:** what lives in the app, what in a notification, a Live Update, a widget, a Tile.

## 4. Decision

Pick one and write three lines: what you chose, which constraint or evidence decided it, and why each alternative lost. This is the part a reviewer reads. A decision without a reason will be undone by the next person.

## 5. Failure states

Design the paths that break. On Android these are concrete:

- **The window changes:** rotation, fold/unfold, split screen, desktop resize, an external display with a different density. State (selection, scroll position, text being typed) must survive, and so must process death.
- **The keyboard appears:** fields and the confirming action stay visible.
- **The person's settings:** text at 200%, display size larger, dark theme, dynamic colors from an unusual wallpaper, animations turned off, a screen reader on, a hardware keyboard and no touch.
- **Permissions denied:** notifications, location, camera. The app still works and explains what is missing at the moment it matters.
- **Empty, loading, error, offline, signed out.** Each has a next action.
- **Back at every level:** a pane collapsing, a sheet closing, a cancelled predictive back gesture returning to where it was.

## 6. Validation

Prove the design on a device or emulator, not in a static frame:

- Resize through every width class boundary and at least one compact-height window. Where a platform rule has a disputed boundary (Android 17's large-screen rule), test both sides of it.
- Run the relevant checklists: `SKILL.md` review list, `quality.md`, the surface file.
- Accessibility pass: TalkBack through the whole task, 200% text, Accessibility Scanner, keyboard-only completion (`accessibility.md` §6).
- For each decision in §4, name the observation that would show it was wrong.

---

## 7. Worked example A: an iOS tab app becomes an adaptive Android app

**Intake.** A recipe app on iOS: tab bar with five tabs (Home, Search, Saved, Shopping list, Profile), large titles, a "Cook" primary button on each recipe, page sheets for editing. Compose, Material 3 1.4 stable, minSdk 26, targetSdk 36. Must run on phones and large screens. Brand has a fixed orange and a custom typeface.

**Constraints.** OS behavior: on large screens the app may not lock to portrait (targetSdk 36 on Android 16 allows a temporary opt-out, which disappears at targetSdk 37), edge-to-edge is enforced, predictive back is on. Quality: large-screen tier 3 expects correct behavior across configuration changes and in multi-window (`quality.md`). Material: a navigation bar holds three to five destinations with labels; a rail serves wider windows.

**Alternatives.**
1. Navigation bar everywhere, stretched on tablets.
2. `NavigationSuiteScaffold`: bar on compact width or compact height, rail otherwise; Search becomes a search bar on Home on large windows.
3. Drawer on all sizes.

**Decision.** Option 2. Five destinations fit a bar; the scaffold's default switches on width *or* height, so a landscape phone keeps the bar. A stretched bar wastes the large screen and moves controls far from the hands; a drawer hides five equal destinations behind a tap. The "Cook" button becomes an inline filled button on the recipe, not a FAB: it confirms a specific item rather than starting the screen's main creation task. Page sheets for editing become full-screen dialogs on compact width and a dialog or side sheet on larger windows, keeping the discard-changes rule. Large titles become a medium or large flexible top app bar that collapses on scroll. Brand orange stays on content and primary actions; surfaces use tonal roles; dynamic color is off for brand reasons, with explicit light and dark schemes.

**Failure states.** Rotation mid-edit keeps the draft. In split screen at compact width the bar returns. The shopping list survives process death. With text at 200%, five bar labels may truncate: keep labels one word and verify.

**Validation.** Resize across 600 and 840dp and a compact-height window; TalkBack through cook mode; check the orange on surfaces in both themes for 4.5:1.

## 8. Worked example B: an inbox across window sizes and a fold

**Intake.** A messaging inbox and conversation view. Compose, material3-adaptive 1.3. Phones, tablets, a book-style foldable, desktop windows with mouse and keyboard.

**Alternatives.** (1) Separate list and detail screens at every size. (2) `ListDetailPaneScaffold`: one pane on compact, both panes when the window allows. (3) Three panes on very wide windows (folders, list, conversation).

**Decision.** Option 2 now, with option 3 on extra-large widths only if folders are a frequent task. List-detail is Google's canonical layout for this content, and the scaffold handles pane-specific Back: on a compact window Back from a conversation returns to the list, on an expanded window Back does not "close" a pane that is still visible.

**Failure states.** Unfolding while reading keeps the same conversation open and the list scrolled to it. Folding back to compact shows the conversation, not the list (the person was reading it). The keyboard in the reply field does not cover the send button. On desktop, hover reveals selection checkboxes and right-click opens a menu; keyboard shortcuts cover reply and archive. With a separating hinge, no content straddles it.

**Validation.** Fold/unfold mid-reply; drag the desktop window through every class; keyboard-only: open, reply, archive.

## 9. Worked example C: a Wear OS Tile for the same product

**Intake.** The recipe app wants "next step" and a timer on the watch.

**Alternatives.** (1) A full Wear app. (2) A Tile showing the current step and timer, opening the app for more. (3) Only an ongoing notification bridged from the phone.

**Decision.** A Tile plus an ongoing activity for the running timer. A Tile is glanceable and swipeable from the watch face; the full app is for deeper interaction. Text meets Wear's minimum sizes, touch targets are 48dp, and the layout uses percentage margins so it holds on small and large round screens (`wear.md`).

**Failure states.** The phone is out of range: the Tile shows the last known step and says it is not live. The timer finishes while the screen is in ambient mode.

**Validation.** Small (under 225dp) and large round emulators; ambient; disconnected phone.

---

## 10. The deeper review lens

After the mechanical checklist, ask:

- **Is the person in charge?** Their text size, theme, colors, language and animation setting all win over the design.
- **Does the design survive change?** Every window, posture and input the device can produce, including the ones nobody mocked up.
- **Is every number tied to a surface and a version?** A rule quoted without its gate is a bug waiting for a different device.
- **Is expression earning its place?** Each expressive moment should make something easier to notice, understand or enjoy; otherwise dial it down.
- **Would a TalkBack or keyboard user finish the task as fast as a touch user?**

## 11. Sources

- Translate designs to Android: https://developer.android.com/design/ui/mobile/guides/foundations/translate-designs
- Window size classes: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Adaptive navigation: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Behavior changes, Android 16: https://developer.android.com/about/versions/16/behavior-changes-16
- Adaptive app quality: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
- Wear OS app quality: https://developer.android.com/docs/quality-guidelines/wear-app-quality
