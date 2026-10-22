# App widgets

**Applies to:** home-screen and lock-screen-capable app widgets on phones, tablets and foldables, built with Jetpack Glance (Compose-style) or RemoteViews; widgets also appear on Android Auto. Quality IDs come from Google's widget quality checklist.

A widget is a small, live window into one part of the app, sized and placed by the person and rendered by the launcher. It is not a miniature app.

## Contents

1. Decide the use case
2. Canonical layouts
3. Sizing
4. Style
5. Configuration and discovery
6. Quality tiers
7. Sources

---

## 1. Decide the use case

- **One primary use case per widget** (the next event, a shortcut to compose, the current playlist). Several use cases mean several widgets.
- Ask: what does the person want to know or do without opening the app? If the answer changes rarely, a shortcut may be enough; if it needs input, a widget may not fit.
- Google's start sequence: pick the use case, check the Figma templates, design within the sizing guide, test sizes, test form factors (including Android Auto), meet the widget quality guide.

## 2. Canonical layouts

Google publishes four canonical layouts: **text**, **toolbar**, **list** and **grid**, with Figma templates and a Glance implementation. Start from one. Android 17 adds full-bleed widgets with snap scrolling (Glance 1.3.0-alpha02+, compileSdk 37, AGP 9.1.0+). Widgets on Android Auto don't scroll.

## 3. Sizing

- The **launcher decides the size** from the grid; you set a minimum, a target and resize ranges, then design for every size in between. Content responds: show more at larger sizes, never just stretch.
- Approximate size ranges differ widely between devices: a handheld 2x1 cell spans about 109–306 x 56–130dp, a handheld 4x3 about 245–624 x 185–422dp; tablets differ again. Test at the smallest and largest end of each range.
- Set `targetCellWidth` / `targetCellHeight` (Android 12+) and fill the widget's bounds (no empty margins inside the frame).
- Offer resizing; Google's quality guide expects common sizes such as 2x2, 4x1 and 4x2 where the content allows.
- Tap targets stay **48dp** even at the minimum size.

## 4. Style

- Use the system corner radius (`system_app_widget_background_radius`) so widgets match the launcher, and the inner radius for nested content.
- **Dynamic color** on Android 12+, with a fallback; support **dark theme**. Android 16 QPR2 expands dark theming and custom icon shapes on the user side, so test themed results.
- Text and icons need the same contrast as in the app (`accessibility.md` §2).
- Keep branding small: the content is the brand.

## 5. Configuration and discovery

- **Configuration:** one or two screens at most, with a live preview; offer 6–8 variations to pick from; reconfiguration available later (long press). Make configuration optional where a sensible default exists.
- **Discovery:** always ship a preview (`android:previewLayout`, Android 12+) so the picker shows the real widget; set size hints per form factor; offer to pin the widget (`requestPinAppWidget`) at a contextual moment, not on first launch.

## 6. Quality tiers

Google's widget quality checklist groups items by layout (WL), color (WC), discovery (WD), content (WT) and system integration (WS), in three tiers from a low-quality floor to differentiated widgets. The items in this file (fills its bounds, resizable, themed, preview present, 48dp targets, configurable, responsive at every size) map onto those groups; read the checklist for the tier each item sits in before signing off a tier.

## 7. Sources

- Widgets design: https://developer.android.com/design/ui/mobile/guides/widgets
- Widget layouts: https://developer.android.com/design/ui/mobile/guides/widgets/layouts
- Sizing: https://developer.android.com/design/ui/mobile/guides/widgets/sizing
- Style: https://developer.android.com/design/ui/mobile/guides/widgets/style
- Configuration: https://developer.android.com/design/ui/mobile/guides/widgets/configuration
- Discovery and promotion: https://developer.android.com/design/ui/mobile/guides/widgets/discovery-promotion
- Widget quality: https://developer.android.com/docs/quality-guidelines/widget-quality
- Glance canonical layouts samples: https://github.com/android/platform-samples/tree/main/samples/user-interface/appwidgets
