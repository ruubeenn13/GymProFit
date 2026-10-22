# Components

**Applies to:** Material 3 components in Compose (Material 3 1.4 stable, 1.5 for Expressive-only items) and MDC-Android for Views. Numbers are Material's guidance unless marked as a library default. iOS equivalents are given for orientation; the decision procedure for porting is in `from-ios.md`.

Reach for the standard component first: it brings states, motion, accessibility semantics, predictive back and adaptive behavior with it. A custom component has to rebuild all of that.

## Contents

1. Navigation: bar, rail, drawer, tabs, top app bars, toolbars, search
2. Actions: buttons, button groups, split button, FAB, FAB menu, icon buttons, menus
3. Containment: cards, lists, carousel, dialogs, sheets, snackbar, tooltips
4. Selection and input: switch, checkbox, radio, chips, sliders, text fields, pickers
5. Communication: badges, progress and loading
6. Sources

---

## 1. Navigation

**Navigation bar** (iOS: tab bar). Three to five top-level destinations on compact windows. Labels always visible, one or two words. The active destination uses a filled icon and an indicator. Reselecting the current destination scrolls to top or returns to its root. Don't use it for actions, don't swipe between destinations, and don't hide it on scroll while a screen reader is running. Wider windows switch to a rail (`adaptive.md` §2).

**Navigation rail** (iPadOS: sidebar, collapsed). Three to seven destinations on the leading edge of medium and wider windows; the FAB, if any, sits at its top. The **expanded rail** shows labels beside icons and can hold secondary sections; the modal expanded rail overlays content on smaller windows and supports predictive back.

**Navigation drawer.** For many destinations or deep hierarchies; modal on compact windows, standard (persistent) on wide ones. Prefer the expanded rail in new Expressive designs. The adaptive scaffold never picks a drawer automatically.

**Tabs** (iOS: segmented control used for views). Sibling views of the same content within one destination (for example "Upcoming / Past"). Primary tabs sit under the top app bar; secondary tabs within content. Swiping between tabs is expected. Not for top-level navigation.

**Top app bars** (iOS: navigation bar with title). Small, medium-flexible and large-flexible (the older fixed medium/large are no longer recommended), plus a search app bar. One primary action, up to about two trailing icons; avoid stuffing an overflow menu, prefer a toolbar for more actions. Medium and large collapse to small on scroll. The Up arrow appears on screens below the top level.

**Toolbars** (iOS: bottom toolbar). New in Expressive, replacing the bottom app bar. *Docked* spans the bottom edge; *floating* sits above content (16dp horizontal margin) and can pair with a FAB. Don't combine a docked toolbar with a navigation bar on the same screen. 48dp targets.

**Search.** A search bar on the screen where search starts, expanding to a search view with suggestions; on large windows it can stay inline. Results update as the person types where feasible.

## 2. Actions

**Common buttons:** filled (highest emphasis, one per area), tonal, elevated, outlined, text (lowest). Sentence-case labels of one to three words, an optional leading icon. Buttons can be round or square, and toggle buttons morph shape when selected (Expressive). Several sizes exist from extra-small to extra-large; use larger sizes for prominent actions and touch-heavy contexts.

**Button groups** (Expressive). *Standard* groups lay related buttons together and react to presses. *Connected* groups replace segmented buttons for choosing among options (single or multiple selection). Decide and record whether a group selects or acts.

**Split button** (Expressive). A primary action plus a menu of related variants ("Send" + "Schedule send").

**FAB** (iOS: no direct equivalent; a prominent primary action). For the screen's single most common *creating or starting* action (compose, add, navigate). One per screen; bottom trailing on compact and medium windows, at the top of the rail on wider ones. Medium and large FABs; the small FAB is no longer recommended. The extended FAB adds a label. Not for confirming a form or destructive actions; a form's submit is an inline filled button.

**FAB menu** (Expressive). A FAB that opens two to six related actions with labels. Don't combine with a toolbar, or with an extended FAB.

**Icon buttons:** standard, filled, tonal, outlined, toggleable; always with an accessible name. 48dp target even at small visual sizes.

**Menus** (iOS: context menu, pull-down). Temporary lists of actions or choices, opened from a button, a long press or right-click. Keep them short; group with dividers; destructive items last.

## 3. Containment

**Cards:** elevated, filled, outlined. For a single subject with mixed content and actions. Don't force every list into cards; implicit grouping (space, alignment) comes first.

**Lists** (iOS: table cells). One to three lines per item, leading and trailing elements, row height set by the tallest element. The most common component on Android; use it before inventing a layout.

**Carousel.** A scrolling row of items with size changes as they move (multi-browse, uncontained, hero, full-screen variants). For browsing visual collections, not for critical information.

**Dialogs** (iOS: alert). Interrupting decisions only. Title, short supporting text, up to two actions, the confirming action trailing; never disable the dismissive action. A **full-screen dialog** is a compact-window option for a multi-field task (iOS: page sheet); on larger windows use a dialog or side sheet. Preserve unsaved-work rules: closing with changes asks before discarding.

**Bottom sheets** (iOS: sheet with detents, action sheet). Standard (coexists with content) and modal (blocks it, mobile-sized windows). Open at a sensible initial height; drag handle; predictive back collapses or dismisses. **Side sheets** take over on expanded windows.

**Snackbar.** Brief feedback about an action, at most one at a time, one optional action ("Undo"), above FABs and bottom bars; auto-dismisses after a few seconds, so never put the only path to something important in it. Screen readers need enough time (or no timeout) for the action.

**Tooltips.** Plain (a short label for an icon) or rich (title, text, action). Hover and long-press triggered.

## 4. Selection and input

- **Switch** (iOS: toggle) for an immediate on/off setting; **checkbox** for one or more choices that are confirmed later or within a list; **radio buttons** for one choice among a few visible options.
- **Chips:** assist, filter, input, suggestion; filter chips toggle and can show a check.
- **Sliders:** continuous or discrete, with value labels; Expressive adds more sizes and orientations. Always offer a precise alternative where values matter.
- **Text fields:** filled or outlined; label, supporting text, error text that says how to fix the error; the right keyboard type; autofill hints; a secure field for passwords.
- **Date and time pickers:** modal or docked; allow typed input as well as calendar/clock selection.

## 5. Communication

- **Badges:** a dot or a count on navigation items and icons; keep counts short (cap them).
- **Progress indicators:** linear and circular, determinate when the duration is known; Expressive adds wavy variants.
- **Loading indicator** (Expressive): a shape-morphing indicator for short, indeterminate waits. Material's guidance: for very short waits (about 200ms or less) just show the content; for longer waits with known progress, use a progress indicator instead.

## 6. Sources

- Components index: https://m3.material.io/components
- Navigation bar: https://m3.material.io/components/navigation-bar/guidelines
- Navigation rail: https://m3.material.io/components/navigation-rail/guidelines
- App bars: https://m3.material.io/components/app-bars/guidelines
- Toolbars: https://m3.material.io/components/toolbars/guidelines
- Buttons: https://m3.material.io/components/buttons/guidelines
- Button groups: https://m3.material.io/components/button-groups/guidelines
- Split button: https://m3.material.io/components/split-button
- FAB: https://m3.material.io/components/floating-action-button/guidelines
- FAB menu: https://m3.material.io/components/fab-menu/guidelines
- Dialogs: https://m3.material.io/components/dialogs/guidelines
- Bottom sheets: https://m3.material.io/components/bottom-sheets/guidelines
- Side sheets: https://m3.material.io/components/side-sheets/guidelines
- Snackbar: https://m3.material.io/components/snackbar/guidelines
- Lists: https://m3.material.io/components/lists/guidelines
- Cards: https://m3.material.io/components/cards/guidelines
- Loading indicator: https://m3.material.io/components/loading-indicator/guidelines
- Compose Material 3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3
