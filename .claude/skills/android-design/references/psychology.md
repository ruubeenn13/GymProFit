# Psychology: heuristics that change a decision

**Applies to:** design reviews and trade-offs on any surface. Each heuristic is listed only if it changes a concrete decision. Each one says what the evidence supports and where it stops. No rule in this skill is "derived" from a law: Google's numbers (five navigation destinations, 48dp targets) come from Google's guidance and testing, and the heuristics here only explain why they tend to hold.

*Author synthesis, drawing on well-established human–computer interaction findings and on Google's published Material research where cited.*

## Contents

1. Target size and reach
2. Choice and recognition
3. Attention and glanceability
4. Change and motion
5. Defaults and effort
6. Trust and timing
7. Sources

---

## 1. Target size and reach

- **Finding:** larger and closer targets are hit faster and more accurately (Fitts's law, among the most replicated results in HCI).
- **Decision it changes:** put frequent actions in the thumb's natural range on compact windows (bottom bar, FAB, bottom sheets); make frequent targets bigger than the minimum; keep destructive actions away from frequent ones.
- **Limits:** it predicts pointing time, not the right minimum size; the 48dp floor is Google's accessibility guidance. On large windows and with a pointer the reach argument mostly disappears, which is why the navigation moves to a rail.

## 2. Choice and recognition

- **Finding:** people recognize more easily than they recall; more options take longer to choose from, especially when they are unfamiliar (Hick–Hyman law).
- **Decision it changes:** keep navigation labels visible (Google's expressive research found removing labels lowered usability); show the few most likely choices first and put the rest behind progressive disclosure; use familiar icons for familiar actions.
- **Limits:** the cost of extra options is small for well-ordered, familiar lists (an alphabetical contact list is fine). "Seven plus or minus two" is about short-term memory, not menus; don't use it to cap anything.

## 3. Attention and glanceability

- **Finding:** attention is limited and shared; people looking at a watch, a car display or glasses give it a second or two at a time.
- **Decision it changes:** one primary piece of information per glance on Wear tiles, car screens and AI glasses; shorter text; voice first in cars; no automatic transition faster than a person can glance twice (the car guidance waits at least 8 seconds).
- **Limits:** on a phone in a calm setting people read and compare; don't strip a reading screen to glance-level content.

## 4. Change and motion

- **Finding:** people miss changes that happen without a cue (change blindness); motion that connects a before and after state helps people follow what happened.
- **Decision it changes:** animate container transforms and shared elements when a screen changes structure; announce changes to screen-reader users; make sure the end state is clear with animations turned off.
- **Limits:** motion costs time and can make some people unwell; Google's expressive research shows springy motion can feel delightful, but on repeated tasks, shorter is better.

## 5. Defaults and effort

- **Finding:** most people keep defaults; every extra step loses some of them.
- **Decision it changes:** choose defaults that suit most people (polite defaults), so settings stay rare; ask for an account or a permission only when it buys the person something; let onboarding be skipped.
- **Limits:** a default that serves the business against the person (pre-ticked marketing consent) breaks trust and, in many places, the law and Play policy.

## 6. Trust and timing

- **Finding:** a request makes more sense, and is accepted more often, when its reason is visible at that moment.
- **Decision it changes:** permission prompts after the person starts the feature (`patterns.md` §4); notification permission after a moment that shows its value; rating prompts only through the In-App Review API at a natural pause.
- **Limits:** context doesn't excuse asking for more than the feature needs; Android stops showing the dialog after two denials, so a badly timed ask is costly.

## 7. Sources

- Expressive design research (labels, familiar patterns, preference limits): https://design.google/library/expressive-material-design-google-research
- Permissions requesting guide: https://developer.android.com/training/permissions/requesting
- Car UX requirements: https://developer.android.com/design/ui/cars/guides/ux-requirements/overview
- Material motion: https://m3.material.io/styles/motion/overview
