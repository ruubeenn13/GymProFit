# Android XR headsets, XR glasses and AI glasses

**Applies to:** Android XR (headsets and wired XR glasses) with the Jetpack XR SDK, and AI glasses (audio-only and display) with Jetpack Compose Glimmer. Both SDKs are young: check `versions.md` and the source pages before relying on an API. Large-screen Android apps run on XR as 2D panels, so tier 2 adaptive work (`adaptive.md`, `quality.md`) comes first.

## Contents

1. Devices and what they change
2. Panels, Home Space and Full Space
3. Targets, type and states
4. Motion and comfort
5. XR quality
6. AI glasses
7. Sources

---

## 1. Devices and what they change

| Device | Display | Input | Design consequence |
|---|---|---|---|
| Headset | opaque, true black, ~110°+ field of view | hands (pinch), eyes, voice, 6DoF controllers, keyboard and mouse | full spatial design possible; offer passthrough |
| Wired XR glasses | additive (black renders transparent), ~50–70° field of view | phone, keyboard, other peripherals | dark backgrounds disappear; keep content central and compact |
| AI glasses | none (audio) or a small display | tap, swipe, voice, camera button | glanceable, transient, competes with the real world (§6) |

Center interactive elements, support seated, standing and reclined use, keep a stationary frame of reference, and limit the choices shown at once.

## 2. Panels, Home Space and Full Space

- **Home Space:** apps run as panels side by side with others. A large-screen app opens as a **1024x720dp** panel (minimum 385x595dp, maximum 2560x1800dp), launched about 1.75 m away; that panel reads as roughly 1.56 x 1.09 m. Compatible large-screen apps must work at this size.
- **Full Space:** one app takes over; it can use spatial panels, 3D content and environments. Spatial UI is available in Full Space only.
- Conversion: 1dp ≈ 0.868 mm at 1 m (dp-to-dmm). Keep primary content within the central 41° and slightly below the eye line.
- Panel corners are 32dp. **Orbiters** (controls that float outside a panel, about 20dp offset) hold navigation and controls so they don't cover content. Spatial elevation steps: 0/16/24/32/40/56dp.
- Concurrent tasks get separate panels. Don't letterbox content.

## 3. Targets, type and states

- **Targets:** 56dp recommended (48dp minimum) with Jetpack XR's scaling; icons 48dp; at least 8dp between targets. The quality formula is distance (m) x 0.868 x 48 (minimum) or x 56 (recommended).
- **Type** at least 14dp, normal weight or heavier, sentence case; allow text scaling; keep text off moving objects.
- **Hover and focus states are required.** With eye tracking, the system draws the hover highlight itself and hides where the person is looking from the app.
- Everything must be usable with hands alone.
- Material components work; keep an existing app's design language and support both themes.

## 4. Motion and comfort

- Comfort first: motion is initiated by the person, never forced. Don't rotate the camera.
- When the world must move: fade out and in, hide the periphery, accelerate slowly, offer rest stops, and make motion optional.
- No strobing above three flashes per second, with a warning and a setting to disable flashing content.
- Respect the boundary (2.0 m radius; passthrough fades in from 1.5 m).

## 5. XR quality

From Google's Android XR quality guidelines (no item IDs): large-screen apps at tier 2 or tier 1; hand-only use; frame time under 11.1 ms at 90 Hz (13.8 ms at 72 Hz); cold start under 2 s, warm under 1 s; momentum scrolling; video controls outside the video; dim passthrough or pick an environment in Full Space.

## 6. AI glasses

- **Principles:** multimodal, glanceable and transient; the UI competes with the real world, so show less and for less time. Design the audio flow and the visual flow; audio-only glasses have no display at all.
- **Entry point:** usually handoff from the phone, implicit (navigation starts) or explicit (a button with a glasses icon, optionally "Launch on glasses").
- **Layout:** build from the bottom up and anchor to the bottom of the view; never center vertically; minimal item counts; the system bar is reserved for notifications; the display turns off after a timeout the person sets.
- **Type:** Google Sans Flex; sizes in angular degrees, at least 0.6° and mostly 0.7–0.9° (about 18px at 30 pixels per degree); rounded, heavier, wider-spaced letterforms because light halates on the lenses; no thin or condensed faces.
- **Input:** tap to confirm, swipe, swipe down for back, touch and hold for Gemini, two-finger volume, camera button.
- **Permissions** are granted on the phone; prime the person first and let them change it later.

## 7. Sources

- Design for Android XR: https://developer.android.com/design/ui/xr
- XR immersive get started: https://developer.android.com/design/ui/xr/guides/get-started
- XR foundations: https://developer.android.com/design/ui/xr/guides/foundations
- Spatial UI: https://developer.android.com/design/ui/xr/guides/spatial-ui
- Visual design: https://developer.android.com/design/ui/xr/guides/visual-design
- Motion: https://developer.android.com/design/ui/xr/guides/motion
- Android XR quality: https://developer.android.com/docs/quality-guidelines/android-xr
- AI glasses: https://developer.android.com/design/ui/ai-glasses
- AI glasses app surface: https://developer.android.com/design/ui/ai-glasses/guides/surfaces/app
- AI glasses type: https://developer.android.com/design/ui/ai-glasses/guides/styles/type
- AI glasses permissions: https://developer.android.com/design/ui/ai-glasses/guides/interaction/permissions
