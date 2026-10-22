# Cars: Android Auto and Android Automotive OS

**Applies to:** apps built with the Car App Library (templates) for Android Auto and Android Automotive OS, plus media and messaging apps that use the system's car UI. "Apps in cars" categories (video, games, browsers) are usable only while parked. Strength words below follow Google: MUST items come from the UX requirements and car app quality checklist; SHOULD items are recommendations.

The driver's attention belongs to the road. A car app is a short, glanceable, mostly voice-led task that can pause at any moment and resume when the car stops.

## Contents

1. Principles
2. Templates instead of layouts
3. Driving state and task limits
4. Visual rules
5. Quality checklist
6. Sources

---

## 1. Principles

- Reach any task in **three taps or fewer** where possible.
- **Glanceable text:** one to three lines, about 120 characters at most.
- **Voice first** for input; no typing while driving.
- **Pause and resume:** every task can stop when driving demands it and continue from the same place.
- **Adapt to the driving state:** show more when parked, less when moving.

## 2. Templates instead of layouts

You don't lay out screens in a car: you pick templates, and the host (Android Auto or the car's system) draws them in the car's style, sizes and input method (touch, rotary, touchpad).

- **General:** List, Grid, Sectioned Item, Pane, Message, Long Message, Search, Sign-in, Tab.
- **Special:** Navigation, Place List (with map), Map + Content, Media Playback.
- Each template has item limits that the host enforces (and may lower while driving); check the template page rather than assuming a number.
- Your design decisions are content, order, icons and wording, not pixels.

## 3. Driving state and task limits

- A task flow has at most **5 steps** (MUST); two or three is the target. Longer tasks are parked-only, or pause while driving and resume when parked. Google's checklist states the same limit as tasks of five screens or fewer (AC-1).
- **Long Message** and **Sign-in** templates are parked-only; lists show more text when parked.
- Wait at least **8 seconds** before any automatic transition (SHOULD), so a glance never misses a screen.
- No setup flows, fine-grained controls, payment-method setup, or recurring or multi-item purchases while driving; purchases use existing payment methods. Sign in on the phone or while parked.
- Video, games and browsers can't be used while driving (DD-2/3).

## 4. Visual rules

- Light and dark themes (TH-1; Car App Library 1.9+). Contrast high enough for daylight and night driving (VD-1..3).
- Icons are white sets that the system colorizes; at most **two accent colors**.
- **No animation** while driving (SA-1), except map canvas updates; no auto-scrolling text (ST-1).
- Images only for context or background, drawer icons and driving aids (IU-1).
- **Maps:** text at least **24dp** (SHOULD); markers at least 36x36dp; weather or data overlays keep to three legend entries or colors and five annotations (WE-2, WE-5).
- For the parked-only app categories, Google's checklist sets touch targets of at least 64dp, spaced 24dp apart and from edges, and text of at least 24sp (UX-1..3).

## 5. Quality checklist

Car app quality items to test (IDs from Google's checklist):

- **DR-1** buttons respond within 2 s; **DR-2** the app launches within 10 s; **DR-3** content loads within 10 s.
- **AC-1** tasks fit in five screens.
- **SA-1** no animation while driving; **ST-1** no auto-scrolling text.
- **IT-1** only one-touch controls while driving.
- **TH-1** light and dark themes.
- **LS-U4** tab navigation works with rotary controllers; **LS-I3** keyboard, mouse and trackpad input where the car has them.

Test on the Desktop Head Unit (Android Auto) and an Automotive OS emulator, in both driving and parked states, with touch and rotary input.

## 6. Sources

- Design for cars: https://developer.android.com/design/ui/cars
- UX requirements: https://developer.android.com/design/ui/cars/guides/ux-requirements/overview
- Driving state: https://developer.android.com/design/ui/cars/guides/ux-requirements/driving-state
- Templates: https://developer.android.com/design/ui/cars/guides/templates
- Flows: https://developer.android.com/design/ui/cars/guides/flows/overview
- Car app quality: https://developer.android.com/docs/quality-guidelines/car-app-quality
