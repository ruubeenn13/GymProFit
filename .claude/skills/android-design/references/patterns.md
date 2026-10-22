# Patterns

**Applies to:** phone, tablet, foldable and desktop apps; notification rules apply wherever the app posts notifications (Wear and Auto bridge them). Platform gates are named per item.

## Contents

1. Navigation, Back and Up
2. Settings
3. Onboarding and sign-in
4. Permissions and privacy in the interface
5. Notifications
6. Live Updates
7. Loading, empty, error and offline
8. Writing
9. Sources

---

## 1. Navigation, Back and Up

- Every app has a **fixed start destination**: the first screen after launch from the launcher, and the last screen before Back leaves the app.
- **Back** retraces the person's history; **Up** (the top app bar arrow) moves up the app's hierarchy and never leaves the app. Within one task they often coincide, but they are different concepts.
- **Deep links** build a synthetic back stack: Back from a deep-linked detail goes to the place it would live in the app, not straight out.
- **Predictive back:** on Android 16 and later, for apps with targetSdk 36 or higher, the system plays back-to-home and cross-activity previews and stops dispatching the old `onBackPressed` callback; in-app destinations should support the gesture preview through the navigation library and components. A cancelled gesture returns exactly to where it was.
- Don't add an on-screen back button that duplicates system Back. Don't trap Back (a "press back again to exit" toast is a known anti-pattern).
- **State preservation:** returning from Recents restores exactly where the person was; returning after a long time may restore approximately.

## 2. Settings

- Few settings: one list. Around fifteen or more: group into subscreens with clear category names. On large windows, use list-detail.
- Put the most used settings first; group related ones; use switches for immediate on/off, a subscreen for complex options.
- Polite defaults: the default should suit most people, so settings stay rare.
- Name settings by what they do, not by implementation ("Download over Wi-Fi only", not "Network policy").
- Settings that live in system settings (notification channels, per-app language, permissions) link there rather than being duplicated.

## 3. Onboarding and sign-in

- Deliver value fast: let people see the product before asking for an account, where the product allows it.
- Show progress, allow skipping and resuming, and never repeat onboarding after an update unless something important changed.
- **Sign-in** through Credential Manager: passkeys, passwords, Sign in with Google and digital credentials in one bottom sheet (it replaces the older Google Sign-In, One Tap and Smart Lock APIs). Google's core quality guideline expects it, plus **Restore Credentials** so the person is signed in on a new device without doing anything (mobile only, one account per app).
- **Passkey copy:** "Create a passkey", "Sign in" (not "Log in"), "Sign-in options"; "passkey" in lowercase; show the password manager's name and created and last-used dates in settings. Good moments to offer one: sign-up, account recovery, right after a password sign-in, and settings.
- **Re-authentication** uses `BiometricPrompt` (strong, weak or device credential). Keep explicit confirmation for purchases; skip it for low-risk views.
- Ask for permissions in context (§4), not as an onboarding gauntlet.

## 4. Permissions and privacy in the interface

Design the flow, including the "no" path, before the screen.

1. **Design the feature to work without the permission** and write down its reduced form (voice input off without the microphone, a manual address field without location).
2. **Ask when the person starts the feature**, never at launch. Google's guide states no "one permission at a time" rule; it says to ask when the feature needs it.
3. **Explain in your own UI first** when the reason isn't obvious (or when `shouldShowRequestPermissionRationale` returns true): say what the access is for and what stays off without it, with a "No thanks" button. The system dialog itself can't be styled.
4. **Design for "Only this time"**: on Android 11+ location, microphone and camera can be granted for a single session.
5. **Respect "no".** Keep what works; mark only the limited part of the screen and name what is unavailable; no full-screen blocker, no nagging, and no deep link to system settings used as pressure. After two denials the system stops showing the dialog, so the first ask must come at the right moment.
6. **While capturing**, show an in-app indicator for camera or microphone use, beside the system's status-bar icon; immersive screens keep clear of `getPrivacyIndicatorBounds()`.
7. **Explain access in the Privacy Dashboard** (Android 12+) by registering a rationale activity for `VIEW_PERMISSION_USAGE`.
8. **Prefer no permission at all:** the photo picker (no storage permission; its item limit comes from `MediaStore.getPickImagesMaxLimit()`, not a fixed number), camera and video capture intents, the contact picker (Android 17), companion device pairing, SMS Retriever or Phone Number Hint, approximate location or typed address.
9. **Location:** approximate (about 3 km²) first, precise (about 50 m) on demand; background location only after foreground access and only for core features (Play policy). Apps targeting Android 17 with only session-based location features must use the system location button (Play policy).
10. **Give back what you don't use:** on Android 13+ an app can revoke its own permissions; say so at the next launch. Android also resets permissions of apps unused for a few months.
11. **Notification permission** (Android 13 and later): ask in context with a benefit statement, never at first launch with no context. Media-session notifications and CallStyle notifications from qualifying self-managed calling apps are exempt; other notifications from such apps are not.

Store policy (prominent disclosure, data safety, background location review) is not design guidance; hand it to the Play kit.

## 5. Notifications

- **Copy:** Google recommends titles of about 30 characters or fewer and text of about 40 characters or fewer, so they stay readable collapsed. These are copy recommendations; wrapping depends on width, font size and template.
- **Actions:** up to 3 in a standard expanded notification; media notifications show up to three collapsed and up to five (with an image) or six (without) expanded.
- **Channels** (Android 8.0 and later) let people tune categories; name them by what they contain, and choose importance honestly (high importance only for things that need attention now).
- **Icons:** the small icon is a monochrome silhouette; the large icon is for people (circular avatar) or content, never for the app's branding.
- **Messaging:** use the messaging style, direct reply and conversation shortcuts, and support bubbles where appropriate.
- **Ongoing notifications:** since Android 14, people can dismiss most ongoing notifications (with exceptions, such as some on a locked device and specific categories). Dismissing does not stop a foreground service, so the app must still offer a real stop action.
- **Don't** notify for ads, "we miss you", rating requests, sync status, or anything the person didn't ask for. Don't put the app name in the title (the system shows it).
- Group related notifications under a summary; set lock-screen visibility (public, private, secret) to match sensitivity.

## 6. Live Updates

Live Updates (Android 16 and later) promote an ongoing notification to a prominent card at the top of the shade, on the lock screen and as a status-bar chip.

- **Use for:** an ongoing, user-initiated, time-sensitive activity with a clear start and end: a ride on its way, a food delivery, active navigation, a running timer.
- **Not for:** ads, routine updates, open-ended monitoring, or **package tracking** (Google lists it as not qualifying). Offer an ordinary notification instead.
- **Eligibility** is checked by the system: an allowed style (standard, big text, call, progress; metric style is Android 17), an ongoing notification requesting promotion with the promotion permission, a title, no custom views, not a group summary, not colorized, a channel above minimum importance. The person and the device manufacturer can still decline promotion, so design a fallback.
- **Chip:** up to 96dp wide. Very short text (under seven characters) shows in full; otherwise use short critical text or a time; a countdown appears when the end is at least 2 minutes away.
- **Android 17** adds semantic color annotations (safe, caution, danger, info) on API 37. Text must keep its meaning without the color.
- Alert only on meaningful changes (the driver arrived), not on drift (ETA moved a minute). Never re-post after the person dismissed it. Provide an unpin action where monitoring was opt-in.

### Help and feedback

Help lives in secondary navigation (overflow menu, bottom of the drawer or settings), labelled "Help" and "Send feedback". Ask for ratings through the In-App Review API, not a custom prompt.

## 7. Loading, empty, error and offline

- **Loading:** show content as soon as any is available; skeletons for structure; a loading indicator for short unknown waits; a progress indicator when progress is known; say what is happening for long waits.
- **Empty:** explain what will appear and offer the action that fills it.
- **Errors:** say what went wrong in plain words and what to do next; keep the person's input; retry where it makes sense. No raw error codes as the only message.
- **Offline:** show local data immediately (offline-first reads); for writes decide per action: *online-only* (disable the control or explain, e.g. a bank transfer), *queued* (analytics; nothing to show) or *local first, then sync* (to-do items; show sync state where conflicts matter). See `architecture.md`.
- Every state above is a field of the screen's UI state, not a one-off popup (`architecture.md`).

## 8. Writing

- Understandable first, then brief. Sentence case for titles, buttons and labels.
- Buttons say what they do ("Delete photo", not "OK"); dialogs explain the consequence.
- Avoid jargon and Latin abbreviations; front-load the important words so text scans.
- Write for translation: no text in images, no concatenated sentence fragments, room for 30–50% longer strings, and test right-to-left.

## 9. Sources

- Navigation principles: https://developer.android.com/guide/navigation/principles
- Predictive back design: https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
- Behavior changes, Android 16: https://developer.android.com/about/versions/16/behavior-changes-16
- Settings: https://developer.android.com/design/ui/mobile/guides/patterns/settings
- Onboarding: https://developer.android.com/design/ui/mobile/guides/patterns/onboarding
- Core app quality: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Notification design: https://developer.android.com/design/ui/mobile/guides/home-screen/notifications
- Notification permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Android 14 behavior changes (dismissible notifications): https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications
- Live Updates (develop): https://developer.android.com/develop/ui/views/notifications/live-update
- Live Updates (design): https://developer.android.com/design/ui/mobile/guides/home-screen/live-updates
- Android 17 summary: https://developer.android.com/about/versions/17/summary
- Content design: https://m3.material.io/foundations/content-design/overview
- Requesting permissions: https://developer.android.com/training/permissions/requesting
- Explaining access: https://developer.android.com/training/permissions/explaining-access
- Alternatives to permissions: https://developer.android.com/training/permissions/evaluating
- Photo picker: https://developer.android.com/training/data-storage/shared/photopicker
- Location permissions: https://developer.android.com/training/location/permissions
- Privacy best practices: https://developer.android.com/privacy/best-practices
- Credential Manager: https://developer.android.com/identity/sign-in/credential-manager
- Restore Credentials: https://developer.android.com/identity/sign-in/restore-credentials
- Passkeys design: https://developer.android.com/design/ui/mobile/guides/patterns/passkeys
- Biometric authentication: https://developer.android.com/identity/sign-in/biometric-auth
- Help content: https://developer.android.com/design/ui/mobile/guides/patterns/help-content
