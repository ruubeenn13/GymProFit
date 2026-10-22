# Quality guidelines

**Applies to:** apps distributed on Google Play and anyone using Google's published quality checklists as a design floor. These are **technical quality guidelines**: they are separate from Google Play's policies and review (covered by the Play kit), and separate from Material's design recommendations.

Read each item as: *requirement → does it apply to this app → how to test it.* Item names follow Google's checklist IDs so findings can be traced.

## Contents

1. Core app quality
2. Adaptive app quality tiers
3. Surface-specific checklists
4. Using the checklists in a review
5. Sources

---

## 1. Core app quality

| Area | Items (Google IDs) | Applies when | Test |
|---|---|---|---|
| Usability | `Consistent_UX`, `App_Switcher`, `Sleep_Resume`, `Lock_Resume` | always | switch apps, lock and unlock mid-task; the app returns to the same state |
| UI | `Display_State_Parity`, `Fullscreen_Display`, `Orientation_Transitions` | always | rotate and fold rapidly mid-task; no lost state, no letterboxing |
| Visual | `Graphic_Quality`, `Line_Length` (about 45–75 characters), `Theme_Support` (light and dark) | always | view each screen in both themes on a large window |
| Navigation | `Back_Button_Nav`, `Back_Gesture_Nav`, `State_Preservation` | always | Back from every screen; no custom on-screen back duplicating system Back; return from Recents restores state |
| Notifications | `Notification_Quality`, `Conversation_Quality` | when the app notifies; messaging apps | channels, grouping, messaging style, direct reply |
| Accessibility | `Touch_Target_Size` (48dp), `Visual_Contrast`, `Content_Description` | always | `accessibility.md` §6 |
| Performance | `App_Startup_Time`, `Rendering_Performance` | always | feedback if start takes long; smooth frames while scrolling |
| Identity | `Credential_Manager`, `App_Logins_Restoration` | apps with sign-in | passkeys and saved credentials via Credential Manager; credentials restore on a new device |

## 2. Adaptive app quality tiers

Google ranks large-screen and adaptive support in three tiers, from the minimum to the best:

- **Tier 3 — adaptive ready (minimum).** Works correctly on large screens and in multi-window: configuration changes and combinations of them, multi-window and multi-resume, camera preview and media projection in any orientation, basic keyboard, mouse/trackpad and stylus input, stylus text input on newer versions. Test on a device or emulator with smallest width 600dp or more.
- **Tier 2 — adaptive optimized.** Layouts designed for large screens rather than stretched, and full keyboard and pointer support. Google's 12 checklist IDs, with an observable test each:

  | ID | Observe |
  |---|---|
  | `Responsive_adaptive_layouts` | layout changes at 600/840dp; canonical layouts where they fit |
  | `UI_Secondary_Elements` | secondary elements have max widths; context menus open next to the item |
  | `Touch_Targets` | 48dp targets |
  | `Drawable_Focus` | a visible focus indicator on every focusable element |
  | `Keyboard_Navigation` | Tab moves in reading order, arrows within groups |
  | `Keyboard_Shortcuts` | select, cut, copy, paste, undo, redo |
  | `Keyboard_Media_Playback` | Space plays and pauses media |
  | `Keyboard_Send` | Enter sends in the primary field |
  | `Keyboard_Exit` | Esc closes the focused temporary UI |
  | `Context_Menus` | right-click opens the item's menu |
  | `Content_Zoom` | Ctrl+scroll or pinch zooms where zoom makes sense |
  | `Hover_States` | hover feedback on every clickable element |

  Google lists test devices for these (a foldable at 841x701dp, tablets at 1024x640 and 1280x800dp, a 1600x900dp Chromebook) and the Pixel Fold, Medium Tablet and Desktop emulators. The page states no Play-ranking consequence for the tiers; don't claim one.
- **Tier 1 — adaptive differentiated.** Experiences that only large screens make possible: foldable postures (tabletop, book), multitasking with drag and drop and multi-instance, desktop-class keyboard and pointer behavior (shortcuts, hover parity, right-click menus, scroll bars), stylus features.

State the target tier in the design brief; review against that tier's page.

### Numbers Play measures: Android vitals

Play's "bad behavior" thresholds, over a 28-day window. Above them, Play may reduce the app's visibility and show a warning on its store listing.

| Metric | Overall | Per phone model | Per watch model |
|---|---|---|---|
| User-perceived crash rate | 1.09% | 8% | 4% |
| User-perceived ANR rate | 0.47% | 8% | 5% |
| Excessive battery usage | 1% | – | 1% |
| Excessive partial wake locks | 5% | – | – |

Rendering targets 60 fps for apps (at least 30 fps for games). These are measurements of the shipped app; they sit beside the design checklists, not inside them.

### Startup is a design topic

- Cold start (a new process) is the case to design for. **TTID** is the first frame, reported by the system; **TTFD** is fully drawn and usable, reported by the app (`reportFullyDrawn()`, or `ReportDrawn` / `ReportDrawnWhen` in Compose).
- Show the screen's structure with placeholders first and fill in data as it arrives; defer what isn't needed for the first screen.
- Use the `SplashScreen` API (required from Android 12; a custom splash Activity adds time). The splash is an icon on a background, not a branded loading screen.

### Review lenses from the quality hub

Google's quality hub has four pillars: core value, user experience, technical quality, and privacy and security. Core value comes first: the app must be useful or fun for its audience, store assets must match the real app, and ratings, engagement and retention (not installs) show value.

## 3. Surface-specific checklists

- **Wear OS app quality** (`wear.md`): text sizes, 48dp targets, swipe-to-dismiss, black backgrounds, ongoing activities; watch-face criteria are a separate set.
- **TV app quality** IDs are in `tv.md`; **car app quality** IDs (DR, AC, SA, ST, IT, TH…) in `cars.md`; **XR quality** in `xr-glasses.md`.
- **Widget quality** (`widgets.md`): tiers for sizing, theming, content and interaction.

## 4. Using the checklists in a review

1. Name the surfaces and the target adaptive tier.
2. Mark each item *applies / doesn't apply (why)*.
3. Test on the device, at the window sizes and inputs the item names.
4. Report failures with the item ID, what you observed, and the fix.
5. Keep three lists apart in the report: **quality guideline failures**, **Material guideline departures** (which may be deliberate), and **Play policy concerns** (hand to the Play kit).

## 5. Sources

- Core app quality: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Adaptive app quality: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
- Tier 3: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-3
- Tier 2: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-2
- Tier 1: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-1
- Wear OS app quality: https://developer.android.com/docs/quality-guidelines/wear-app-quality
- Widget quality: https://developer.android.com/docs/quality-guidelines/widget-quality
- Quality hub: https://developer.android.com/quality
- Android vitals: https://developer.android.com/google/play/vitals
- App startup: https://developer.android.com/topic/performance/vitals/launch-time
- Core value: https://developer.android.com/quality/core-value
- The old large-screen quality page is archived; cite adaptive app quality instead.
