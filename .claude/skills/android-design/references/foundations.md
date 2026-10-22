# Foundations: units, color, type, shape, motion, elevation, icons, states

**Applies to:** all Android surfaces using Material 3 (Compose Material 3; MDC-Android for Views). Token values are from the AndroidX implementation where Material's site and the library differ; such differences are called out as conflicts. Versions: `versions.md`.

## Contents

1. Units and grids
2. Color, dynamic color and brand
3. Typography and font scaling
4. Shape
5. Motion
6. Elevation and surfaces
7. Icons and app icons
8. States, targets and haptics
9. Images
10. Sources

---

## 1. Units and grids

- **dp** (density-independent pixels) for layout: 1dp is one pixel at 160dpi. **sp** for text: equal to dp at default settings, scaled by the person's font size. Never use sp for padding, heights or icon sizes.
- Density buckets mdpi ×1, hdpi ×1.5, xhdpi ×2, xxhdpi ×3, xxxhdpi ×4; prefer vector drawables so you ship one asset.
- An 8dp grid for layout and spacing, 4dp for type, icons and small component parts.
- Margins: 16dp on compact windows, 24dp from medium up (recommendations; scaffolds supply their own).
- Line length: Google's core quality guideline asks for about 45–75 characters per line; Material's layout guidance suggests 40–60 for comfortable paragraphs. On wide windows, constrain the text column rather than letting it stretch.

## 2. Color, dynamic color and brand

**Roles, not hex values.** Material 3 assigns colors to roles: primary / on-primary / primary-container / on-primary-container (and the same for secondary and tertiary, plus "fixed" variants that stay the same in light and dark), error family, surface and on-surface, the surface-container tiers (lowest, low, default, high, highest), surface-dim and surface-bright, inverse roles, outline and outline-variant, scrim and shadow. Components read roles; you pick schemes.

**How schemes are made.** A source color goes through Material Color Utilities into key colors (primary, secondary, tertiary, neutral, neutral-variant), each expanded into a tonal palette, and roles are assigned tones from those palettes. The HCT color space (hue, chroma, tone) keeps tone, and so contrast, predictable. Material defines standard, medium and high contrast levels for schemes. Use Material Theme Builder to generate schemes; do not hand-pick tones.

**Dynamic color** (Android 12 and later) derives the scheme from the person's wallpaper (user-generated) or from content such as album art (content-based). In Compose: `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)`, guarded by an API-level check.

**Brand is legitimate.** Dynamic color is an opt-in decision, not a mandate:
- **Follow the user** when the app is a utility or a personal tool and brand recognition rests on other things (name, icon, voice).
- **Keep brand color** when color *is* the brand or carries meaning; ship explicit light and dark schemes. A common middle path: dynamic surfaces and neutrals, brand primary.
- **Semantic colors stay fixed** in every case: error, success, warning, and any color that encodes meaning (a red "delete", a green "safe" state). Never let the wallpaper decide what danger looks like.
- Check contrast for every scheme you ship *and* for dynamic schemes from unusual wallpapers (very light, very dark, highly saturated).

**Dark theme** is a quality expectation: support both, use surface tones rather than pure black (except on Wear and where OLED black is the design), and never invert brand imagery.

**Never use color alone** to carry meaning; pair it with text, an icon or shape.

## 3. Typography and font scaling

**Type roles.** Fifteen baseline roles in five groups, each large/medium/small: display, headline, title, body, label. Sizes and line heights (sp):

| Role | Size / line height |
|---|---|
| displayLarge / Medium / Small | 57/64 · 45/52 · 36/44 |
| headlineLarge / Medium / Small | 32/40 · 28/36 · 24/32 |
| titleLarge / Medium / Small | 22/28 · 16/24 · 14/20 |
| bodyLarge / Medium / Small | 16/24 · 14/20 · 12/16 |
| labelLarge / Medium / Small | 14/20 · 12/16 · 11/16 |

Material 3 Expressive adds **fifteen emphasized roles** alongside these (heavier weight for hierarchy). Use emphasized styles for the few things that must stand out, not as a second default.

**Weight conflict.** Google's Compose guide lists titleLarge as Medium weight, while the AndroidX baseline token sets titleLarge Regular. This is a documented conflict: in code use the library's `MaterialTheme.typography` tokens rather than retyping weights, and decide deliberately if your design depends on it.

**Typefaces.** Roboto is the system default; Roboto Flex, Roboto Serif, Roboto Mono and Google Sans Flex are Google's recommended variable families. A brand face is fine: map it into the roles (often a "brand" face for display and headline and a "plain" face for body and label).

**Font scaling.** Since Android 14 people can scale text up to 200%, and scaling is **non-linear**: large text grows less than small text so hierarchy survives. Rules:
- Set text size *and* line height in sp.
- Let containers grow: no fixed heights on anything with text; allow wrapping; let lists and buttons get taller.
- Don't treat `fontScale` as a multiplier for dp; convert with the platform APIs if you must.
- Test at 200% *together with* a larger display size setting, and with your longest translation.

## 4. Shape

**Corner scale** (dp): none 0, extra-small 4, small 8, medium 12, large 16, large-increased 20, extra-large 28, extra-large-increased 32, extra-extra-large 48, plus full (pill). As a list: 0/4/8/12/16/20/28/32/48. Components have their own defaults (buttons are fully rounded by default); use the theme's shape roles rather than raw numbers.

**Expressive shapes.** Material 3 Expressive adds a library of 35 shapes (scallops, cookies, pills, bursts and others) with built-in morphing between them. Use shape change to signal state (a toggle button morphing as it is selected, a loading indicator morphing through shapes) or to give a few hero elements personality. A screen where everything has a different shape has no hierarchy left.

## 5. Motion

**Springs.** Material 3's motion system is physics-based: springs defined by stiffness and damping. There are two motion schemes, **expressive** (lively, spatial springs may overshoot) and **standard** (calmer), and each has fast, default and slow speeds in two categories:
- **Spatial** springs move things (position, size, rotation) and may bounce.
- **Effects** springs change color and opacity and should not overshoot.
Material recommends the expressive scheme for most products and standard for utility-focused ones; in Compose, set it through the theme's motion scheme rather than per animation. A library theme's default is not necessarily the recommendation: check what your theme sets.

**Easing and duration tokens** remain for duration-based animation: emphasized, emphasized-decelerate, emphasized-accelerate, standard, standard-decelerate and standard-accelerate curves; durations short 50/100/150/200 ms, medium 250/300/350/400 ms, long 450/500/550/600 ms, extra-long 700/800/900/1000 ms. Pair longer durations with larger distances.

**Transition patterns:** container transform (an item becomes its detail), shared axis (forward/back along a sequence), fade through (unrelated destinations), top-level fades between navigation destinations, enter/exit for things appearing on screen.

**Turning motion off.** People can remove animations in accessibility settings. In Compose, animation duration is governed by `MotionDurationScale` in the coroutine context: a scale of 0 makes animations finish on the next frame, and the system setting feeds it for standard animation APIs. In Views, check `ValueAnimator.areAnimatorsEnabled()` (API 26). Neither guarantees that *custom* animation code respects the setting; test with animations off and make sure nothing depends on seeing motion (a state change must also be visible in its end state).

## 6. Elevation and surfaces

- Elevation in Material 3 is expressed mainly through **tone**: higher surfaces use a higher surface-container role, and shadows are optional. Levels 0–5 map to 0/1/3/6/8/12 dp.
- Keep component defaults (a FAB is higher than a card) and avoid inventing new levels.
- Hierarchy on Android comes from tonal surfaces, containment and type, not from translucency. Translucency is used where the system uses it (status-bar protection, scrims), not as a general material.

## 7. Icons and app icons

**Material Symbols** is the icon system: a variable icon font (outlined, rounded, sharp) with fill, weight, grade and optical-size axes. Match icon weight to adjacent text, use the filled variant for selected states (a selected navigation item), and use the standard symbol for a standard action. Compose Material 3 1.4 dropped the old material-icons dependency in favor of Symbols.

**Adaptive launcher icon:**
- Two layers (foreground and background), each 108x108dp; vectors preferred.
- Keep the logo inside the central 66x66dp safe zone (the logo itself typically 48–66dp); the outer 18dp band on each side is reserved for the launcher's masks and motion effects. These are different measurements: 108 minus two 18dp bands is 72, which is not the safe zone.
- Provide a **monochrome** layer so the icon can be themed (Android 13 and later, when the user enables themed icons and the launcher supports it). From Android 16 QPR2 the system can theme icons that lack a monochrome layer, with results you don't control; supply your own.
- No baked-in masks, rounded corners or drop shadows.

**Play Store icon:** 512x512, 32-bit PNG, sRGB, up to 1024 KB, a full square: Play applies rounded corners with a radius of 30% of the icon size and its own shadow. No ranking, price or promotional text in the icon. (Store listing assets are covered by the Play kit.)

**Splash screen:** the system splash shows the launcher icon or a branded icon on a background; keep any animation short, and don't build a second splash screen of your own.

## 8. States, targets and haptics

- **State layers:** hover 8%, focus 10%, pressed 10%, dragged 16% opacity overlays of the content color. Use the components' states rather than custom ones so focus and hover look consistent.
- **Touch targets** at least 48x48dp even when the visible element is smaller (a 24dp icon in a 48dp button), with about 8dp between targets.
- **Haptics:** use the platform's predefined feedback constants for confirm/reject, long press and similar; keep haptics crisp and purposeful; never use buzzy one-shot vibrations for routine taps; respect the person's haptic settings.

## 9. Images

- Use vectors for icons and illustrations; density-specific raster images otherwise.
- Choose content scaling deliberately (crop to fill a frame, fit to show everything).
- Put a scrim under text that sits on images, and test contrast on the lightest likely image.

## 10. Sources

- Grids and units: https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units
- Material 3 in Compose (color, type, shape, dynamic color): https://developer.android.com/develop/ui/compose/designsystems/material3
- Color roles: https://m3.material.io/styles/color/roles
- How the color system works: https://m3.material.io/styles/color/system/how-the-system-works
- Typography overview: https://m3.material.io/styles/typography/overview
- Type scale tokens (AndroidX): https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/TypeScaleTokens.kt
- Shape tokens (AndroidX): https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/ShapeTokens.kt
- Shape overview: https://m3.material.io/styles/shape/overview
- Motion overview: https://m3.material.io/styles/motion/overview/how-it-works
- Motion tokens (AndroidX): https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/MotionTokens.kt
- Elevation tokens (AndroidX): https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/ElevationTokens.kt
- MotionDurationScale: https://developer.android.com/reference/kotlin/androidx/compose/ui/MotionDurationScale
- Non-linear font scaling: https://developer.android.com/about/versions/14/features#non-linear-font-scaling
- Material Symbols: https://m3.material.io/styles/icons/overview
- Adaptive icons: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
- Play icon specification: https://developer.android.com/distribute/google-play/resources/icon-design-specifications
- State layers: https://m3.material.io/foundations/interaction/states/state-layers
- Accessibility (targets): https://developer.android.com/guide/topics/ui/accessibility/apps
- Haptics principles: https://developer.android.com/develop/ui/views/haptics/haptics-principles
- Core app quality: https://developer.android.com/docs/quality-guidelines/core-app-quality
