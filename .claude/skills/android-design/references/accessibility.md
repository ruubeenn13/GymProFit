# Accessibility

**Applies to:** every Android surface. Compose semantics first, Views where different. Accessibility is a set of acceptance criteria, checked on a device: each item below names what you should be able to observe.

## Contents

1. Targets and input
2. Contrast and color
3. Text size and display size
4. Screen readers and semantics (TalkBack)
5. Motion, time and gestures
6. Testing
7. Sources

---

## 1. Targets and input

- **Touch targets at least 48x48dp**, with spacing between them, even when the visible element is smaller. *Observe:* every tappable thing is hit reliably with a fingertip at the edges of its target.
- **Pointer** targets can be smaller than touch targets (Google publishes no single pointer minimum); where touch is also possible, keep 48dp.
- **Keyboard-only completion:** every task can be done with Tab, arrow keys, Enter and Escape; focus is always visible and moves in reading order. *Observe:* unplug touch, finish the main task.
- **Switch Access and Voice Access:** every control is reachable and has a speakable name. *Observe:* "tap Send" works in Voice Access.
- **Alternatives to gestures:** anything done by swipe, long press or drag (swipe to delete, reorder) also has a visible or accessibility-action alternative.

## 2. Contrast and color

- **Text contrast 4.5:1**, or **3:1** for large text, which Android's guidance defines as 18sp regular / 14sp bold and up. Meaningful icons and component boundaries need 3:1. Disabled and purely decorative elements are exempt.
- Check **both themes** and **dynamic color**: a scheme from a pale or saturated wallpaper can drop a pairing below the threshold. Material's high-contrast scheme level exists for people who need it.
- **Never color alone:** errors carry an icon and text; selected states change shape or weight as well as color; charts use patterns or labels.

## 3. Text size and display size

- Text scales **up to 200%**, non-linearly, on Android 14 and later. Set sizes and line heights in sp; never fix the height of a container that holds text.
- People also change **display size** (which scales dp). Test the two together, plus your longest translation.
- *Observe at 200% + largest display size:* nothing clipped or overlapping, no text truncated without a way to read it in full, buttons grow, layouts reflow from rows to columns where needed, and the main action is still reachable without horizontal scrolling.

## 4. Screen readers and semantics (TalkBack)

- **Names describe purpose, not appearance.** An icon button's content description says the action ("Delete message", not "trash can"). Decorative images get no description (`contentDescription = null`). Text that is already visible doesn't need a duplicate description.
- **Merge** related elements into one announcement (an avatar, name and timestamp in a list row read as one item with its actions), using merged semantics; avoid duplicate announcements of the same text.
- **Roles and states:** buttons announce as buttons, switches announce on/off, selected tabs announce selected. Standard components do this; custom ones must set the role and state semantics.
- **Headings:** mark section titles as headings so people can jump between them.
- **Custom actions:** expose swipe or long-press actions (archive, delete, reorder) as accessibility actions on the item.
- **Announce changes:** errors, completed actions and live-changing values (a timer, an upload) are announced, through live regions or by moving focus deliberately; don't announce every tick.
- **Focus restoration:** after a dialog closes, a pane collapses or the window changes size, focus returns to a sensible element, not to the top of the screen.
- **Traversal order** follows the visual reading order; fix it with traversal semantics only when the layout can't express it.
- **Timeouts:** anything that disappears (snackbars, auto-advancing content) gives screen-reader users enough time, or doesn't time out when a screen reader is running.
- **Navigation bars** must not auto-hide while a screen reader is active.

## 5. Motion, time and gestures

- People can **remove animations** in accessibility settings. Compose's standard animation APIs follow it through `MotionDurationScale`; Views can check `ValueAnimator.areAnimatorsEnabled()` (API 26). Custom animation code must be tested with the setting on. *Observe:* with animations off, every state change is still visible in its end state and nothing waits for an animation to be noticed.
- Avoid flashing content, and avoid motion that is the only signal of a change.
- **Right-to-left:** declare `supportsRtl`, use start/end rather than left/right, and check Compose layout direction; mirror directional icons (back arrows) but not media controls or clocks. Test mixed-direction text (a Hebrew sentence with an English product name).

## 6. Testing

1. **TalkBack** through the full task with the screen off or eyes closed.
2. **Switch Access** and **Voice Access** on the main controls.
3. **Keyboard only** (an emulator with a hardware keyboard works).
4. **Accessibility Scanner** on each screen; **Compose UI Check** in Android Studio previews (contrast, text size issues, color-vision previews).
5. **Automated:** Compose UI tests on the semantics tree (names, roles, states, merged nodes) and accessibility lint; Espresso accessibility checks for Views.
6. **Play's pre-launch report** includes accessibility findings (touch targets, contrast, labels).
7. **People:** test with people who use these tools every day; automated tools catch only part of the problems.

## 7. Sources

- Make apps more accessible: https://developer.android.com/guide/topics/ui/accessibility/apps
- Test accessibility: https://developer.android.com/guide/topics/ui/accessibility/testing
- Accessibility design (mobile): https://developer.android.com/design/ui/mobile/guides/foundations/accessibility
- Color contrast (Material): https://m3.material.io/foundations/designing/color-contrast
- Non-linear font scaling: https://developer.android.com/about/versions/14/features#non-linear-font-scaling
- MotionDurationScale: https://developer.android.com/reference/kotlin/androidx/compose/ui/MotionDurationScale
- ValueAnimator.areAnimatorsEnabled: https://developer.android.com/reference/android/animation/ValueAnimator#areAnimatorsEnabled()
- RTL mirroring: https://developer.android.com/training/basics/supporting-devices/languages#MirroringUI
- Core app quality (accessibility items): https://developer.android.com/docs/quality-guidelines/core-app-quality
