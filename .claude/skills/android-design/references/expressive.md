# Material 3 Expressive

**Applies to:** Android phones, tablets and foldables, and Wear OS. Compose: most Expressive APIs live in Compose Material 3 1.5 (beta at the time of writing); Views: MDC-Android 1.14 and later. Check `versions.md` before choosing a component.

## Contents

1. What it is
2. What Google's research found, and its limits
3. What is new
4. The expression dial: how much to use
5. Adopting it in code
6. Expressive review checklist
7. Sources

---

## 1. What it is

Material 3 Expressive is an **expansion of Material 3, not a replacement**. Google announced it on May 13, 2025, alongside Android 16 and Wear OS 6; the system UI on Pixel adopted it in an Android 16 quarterly release. It does not deprecate Material 3: the roles, tokens and components still apply, with more shape, color, type emphasis and motion available on top.

Announcement, OS rollout and library stability are three different dates. A design can be "Expressive" before a project's libraries expose every Expressive component.

## 2. What Google's research found, and its limits

Google reports that Expressive was shaped by 46 studies with more than 18,000 participants. In eye-tracking across ten apps, people found key elements up to 4x faster in the expressive versions, and among 18–24-year-olds the expressive versions were preferred up to 87% of the time; Google also reports higher ratings on attributes such as modernity.

Read these as Google's study results, not as guarantees for your app:
- **Faster spotting is not faster task completion.** It shows that emphasis guides attention.
- **Google's own write-up reports where expression hurt:** a redesign that broke familiar patterns lowered usability, removing text labels lowered usability, and some people preferred the calmer versions. The context mattered (a media app is not a bank).
- The age findings concern **preference**; don't turn them into claims about how older people perform.

The practical conclusion is the skill's synthesis, built on Google's caveats: expression is a dial you set per product and per moment, and basic usability is never traded for it.

## 3. What is new

- **Components:** Google lists 14 new or updated components in the launch set. Among them: button groups (including connected button groups, which replace segmented buttons), split buttons, the FAB menu, docked and floating toolbars (replacing the bottom app bar), a shape-morphing loading indicator, wavy progress indicators, flexible top app bars, and an expanded navigation rail. The exact roster and maturity differ per library version; check before you design around one.
- **Shape:** a library of 35 shapes with morphing between them; larger and more varied corner sizes in the corner scale.
- **Color:** richer use of container colors and tones; contrast levels as tokens.
- **Type:** emphasized versions of every type role for hierarchy.
- **Motion:** a spring-based motion system with expressive and standard schemes (see `foundations.md` §5).
- **Retired or no longer recommended:** segmented buttons (use connected button groups), the small FAB, the bottom app bar (use a toolbar), and the old fixed medium and large top app bars (use the flexible ones). "No longer recommended" in Material's guidance is not the same as `@Deprecated` in code or removal from a library.

## 4. The expression dial

Set the level with four inputs, not with taste alone:

| Input | Push toward restrained | Push toward expressive |
|---|---|---|
| **Task criticality** | money, health, safety, irreversible actions | entertainment, creativity, celebration |
| **Density** | data tables, dense lists, power tools | browsing, onboarding, single-focus screens |
| **Brand** | a calm, trustworthy brand voice | a playful, youthful brand voice |
| **Measured comprehension** | people misread emphasis or miss labels in testing | people find the key action faster with emphasis |

Rules that hold at **every** setting: labels stay (icons alone hurt usability), familiar patterns stay where people rely on them, contrast and target sizes meet `accessibility.md`, and motion can be turned off without losing information.

**A design that fails:** a banking transfer screen with a morphing cookie-shaped "Send" button, emphasized display type for the amount, and a bouncy confirmation. The shape change reads as play on a high-stakes action, the bounce delays the confirmation, and the oversized amount pushes the recipient's name below the fold. The emphasis guides attention to the wrong thing.

**A restrained design that succeeds:** the same transfer with standard type, a clear filled button, the recipient and amount given equal weight, and one expressive moment *after* the transfer completes (a short, calm success animation with a clear end state). Expression is spent where it can't cost anything.

Google's own suggestion is to concentrate expression in a few **hero moments** per product rather than everywhere; treat that as a strong default.

## 5. Adopting it in code

- **Compose:** Expressive APIs were moved out of the 1.4 stable line and continued in 1.5 (now beta). They include `MaterialExpressiveTheme`, the expressive color scheme and motion scheme, and the shape library. Some still need the expressive opt-in annotation; pin the version and compile the snippet (the repo's snippet fixture does this). A project on 1.4 stable can adopt the motion scheme, emphasized typography and many tokens without the beta.
- **Views:** MDC-Android 1.14 (released May 13, 2026) is the first with Expressive themes and components. View components are in maintenance mode as of Android 17, so expect the newest Expressive pieces in Compose first.
- **Wear OS:** Wear Compose Material 3 is a separate library with its own Expressive components (see `wear.md`).

```kotlin
// Compose Material 3 1.5 (beta): expressive theme with a brand color scheme fallback.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else expressiveLightColorScheme(),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
```

## 6. Expressive review checklist

- [ ] The expression level is chosen from task, density, brand and evidence, and written down.
- [ ] Expression is concentrated in a few hero moments.
- [ ] Every icon-only control that people depend on has a visible label or is a universally known symbol with an accessible name.
- [ ] Shape changes signal state or identity, not decoration.
- [ ] Spatial springs move things; color and opacity changes don't bounce.
- [ ] The design still works with animations off and at 200% text.
- [ ] Every Expressive component used exists in the project's pinned library version.
- [ ] Retired components (segmented buttons, bottom app bar, small FAB) are replaced in new work.

## 7. Sources

- Google announcement: https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
- Google research article: https://design.google/library/expressive-material-design-google-research
- Building with M3 Expressive: https://m3.material.io/blog/building-with-m3-expressive
- Shape overview: https://m3.material.io/styles/shape/overview
- Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose Material 3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3
- MDC-Android 1.14.0: https://github.com/material-components/material-components-android/releases/tag/1.14.0
