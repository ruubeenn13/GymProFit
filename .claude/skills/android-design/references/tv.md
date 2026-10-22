# Android TV and Google TV

**Applies to:** apps for Android TV OS and Google TV, built with Compose for TV (`androidx.tv`) or Leanback (Views). Quality items cite Google's TV app quality checklist (May 2026 revision, 81 items in three tiers).

TV is the "10-foot UI": people sit about three meters away, use a D-pad remote, and often share the screen with others in the room. Design for distance, for focus, and for a shared device.

## Contents

1. Layout
2. Focus and navigation
3. Type, color and imagery
4. Icon and banner
5. Playback and ambient
6. Quality tiers
7. Sources

---

## 1. Layout

- Design at a **960x540dp** baseline (mdpi) and supply 1080p-quality assets; Google's layout page builds on that canvas.
- Grid: 12 columns of 52dp with 20dp gutters inside **58dp** side margins; card widths 844 / 412 / 268 / 196dp for one to four cards across.
- **Safe margins — conflict:** the same Google page gives 58dp sides (and 28dp top/bottom) in one place and about 48dp sides / 24dp (5%) top/bottom in another. The 58dp value adds up with the grid to 960dp. Keep all text and controls inside the larger margin; let only backgrounds and artwork bleed to the edge. Nothing may be cut off by overscan (TV-OV).
- **Landscape only** (TV-LO). Content that isn't 16:9 is letterboxed in black.
- Organize browse screens as **categories on the vertical axis, items on the horizontal axis**; avoid nested hierarchies.

## 2. Focus and navigation

- **Every flow works with the D-pad** (TV-DP): up/down/left/right, select, Back, Home, and the mic button. No flow may depend on a Menu button (TV-DM).
- **Exactly one element is focused**, always, and it is obvious: Google's focus system scales the focused item (about 1.025x for large, 1.05x for medium, 1.1x for small elements) and can add a glow. Focus moves to the nearest element in the pressed direction; check that it never jumps unexpectedly or gets stuck.
- **Back always leads toward Home** (TV-DB): a fixed start destination; Back from a deep link returns to the start destination, then out. No exit confirmation dialog, no on-screen Back button, no splash screen in the Back path.
- When a screen shows only confirm, destructive or purchase actions, add a **Cancel** button so a person isn't forced into an action.
- Text entry is painful with a remote: prefer voice, sign-in on a phone (code or QR), and auto-login (Tier 2: TV-LI/LC).

## 3. Type, color and imagery

- Roboto, 15 type styles, buttons in label large; prefer larger sizes than on a phone. Google's TV size table is published as an image and not captured here: read it from the source before fixing sizes.
- Dark themes suit most TV rooms; avoid large pure-white areas and fine detail that shimmers. Colors on TVs vary widely: test on a real panel.
- Imagery carries browsing: large artwork, titles legible over it with a scrim.
- Accessibility (Tier 1, TV-AX): audio description, caption styles, high contrast text, adjustable speech rate; TalkBack works with D-pad focus.

## 4. Icon and banner

- Both `android:icon` and `android:banner` are mandatory.
- **Banner** 16:9: 320x180px at xhdpi (sizes from 160x90 mdpi to 640x360 xxxhdpi). It must show the app name; ship one banner per language if the name changes. Tier 3 asks for a 320x180 banner and an icon at least 160x160 (TV-LB).
- **Launcher icon** 1:1 (80–320px across densities); adaptive icons keep a 72x72 safe zone. No monochrome layer (TV has no themed icons); `roundIcon` is deprecated on TV.

## 5. Playback and ambient

- Remote controls: center pauses and resumes; left/right rewind and fast-forward (TV-NP/PC).
- Picture-in-picture only on explicit user action, with no ads and no controls in the PiP window (TV-IC…IX).
- Ambient mode and screensaver rules (TV-BU/BY/BA): don't keep the screen awake when nothing is playing.

## 6. Quality tiers

- **Tier 3 (required basics):** banner and icon, landscape, overscan, D-pad everywhere, no Menu dependency, Back to Home, playback controls, PiP and ambient rules, `minSdkVersion` 31 or lower (TV-PS), and 64-bit with 16 KB page support (TV-G6).
- **Tier 2:** Baseline Profiles (TV-BP), 4K assets (TV-4K), Cast Connect (TV-CT), sign-in and auto-login (TV-LI/LC), voice (TV-VS/VC).
- **Tier 1:** Engage SDK continue-watching (TV-EC/ER), picture profiles, audio switching, frame-rate matching, pointer and touch with hover (TV-TO), spatial audio, accessibility (TV-AX), AppFunctions (TV-AI).

## 7. Sources

- TV design: https://developer.android.com/design/ui/tv
- Design for TV: https://developer.android.com/design/ui/tv/guides/foundations/design-for-tv
- Navigation on TV: https://developer.android.com/design/ui/tv/guides/foundations/navigation-on-tv
- Layouts: https://developer.android.com/design/ui/tv/guides/styles/layouts
- Focus system: https://developer.android.com/design/ui/tv/guides/styles/focus-system
- Typography: https://developer.android.com/design/ui/tv/guides/styles/typography
- Icon and banner: https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines
- TV app quality: https://developer.android.com/docs/quality-guidelines/tv-app-quality
