# Philosophy: what Google says, and what this skill concludes

**Applies to:** every surface. Read it when a rule seems arbitrary, or when two rules pull in different directions and you need the value behind them to decide.

This file keeps two things apart. **Google's stated values** are paraphrased from Google's pages and cited. **This skill's synthesis** is the authors' reading of those values, labelled as such; use it as a lens, not as a Google rule.

## Contents

1. Google's stated values
2. The research behind Material 3 Expressive
3. This skill's synthesis: eight principles
4. When values conflict
5. Sources

---

## 1. Google's stated values

- **Excellent experiences.** Google's quality hub groups what makes an Android app excellent as delightful, intelligent, adaptive, performant and safe, and sorts the quality work into four pillars: core value, user experience, technical quality, and privacy and security. Core value comes first: an app must be useful or fun for its audience before anything else matters.
- **Material is personal and accessible.** Material 3's principles frame the system around individuals: honor their customization (dynamic color, font size, theme), and make the system usable by everyone. Personalization isn't decoration; it is a reason Material 3 exists.
- **Adaptive by default.** Android runs on phones, foldables, tablets, desktop windows, watches, TVs, cars and headsets, and the window an app gets can change at any moment. Google's guidance treats one adaptive app as the goal, not a phone app with extras.
- **The platform owns the frame.** System bars, Back, notifications, the share sheet, permissions and the photo picker are the system's. Apps work with them (edge-to-edge, predictive back, system pickers) rather than replacing them.
- **Privacy is part of the design.** Ask for less, ask in context, prefer the system's private alternatives, and explain access.
- **Compose first.** Google's direction is Jetpack Compose for new UI; Views remain supported but in maintenance mode.

## 2. The research behind Material 3 Expressive

Google calls Material 3 Expressive its most researched Material update: 46 studies with more than 18,000 participants. In eye-tracking studies, people found key elements up to 4x faster in the expressive designs, and younger participants preferred them up to 87% of the time.

Google's researchers also published the limits, and they matter as much as the headline numbers:

- One size doesn't fit all: a banking task and a media app want different amounts of expression.
- A redesign that broke familiar patterns lowered usability; removing text labels lowered usability.
- No amount of expressive design beats basic functionality.
- A sizeable minority preferred the calmer versions.

This skill's reading: expression is a **dial**, not a default (`expressive.md` §3).

## 3. This skill's synthesis: eight principles

*Author synthesis, built from the values above. Each principle names where it is applied.*

1. **Adapt to the window, not the device.** Decide from the current window size class, posture and input; never from "is this a tablet". (`adaptive.md`)
2. **The person's settings win.** Font scale, display size, theme, dynamic color, contrast and "remove animations" override the mockup. A design that breaks at 200% text isn't finished. (`accessibility.md`)
3. **Expression is a dial.** Set it by task criticality, density and brand; accessibility holds at every setting. (`expressive.md`)
4. **One navigation model.** Back is universal and predictive; Up follows the hierarchy; the start destination is fixed. (`patterns.md` §1)
5. **Tone and surfaces carry hierarchy.** Containers, tonal elevation and type roles show structure; shadows and lines are the exception. (`foundations.md`)
6. **Motion explains change, and can be turned off.** Every animation answers "what just happened?", and the end state alone must still make sense. (`foundations.md` §5)
7. **Glanceable, interruptible, resumable.** People are interrupted by calls, folds, car journeys and process death. State survives all of them. (`architecture.md`)
8. **Quality guidelines are the floor, not the goal.** Meet Google's checklists, then design for the person. (`quality.md`)

## 4. When values conflict

- **Brand vs personalization:** keep brand primary, let dynamic color drive neutrals and secondary roles; fix semantic colors (error, success). (`foundations.md` §2)
- **Expression vs efficiency:** dial expression down on dense, repeated or high-stakes screens; spend it on moments of meaning (first run, completion, empty states).
- **Consistency with iOS vs consistency with Android:** a product looks like itself through content, voice, color and illustration; its controls and navigation follow the platform the person is holding. (`from-ios.md`)
- **Simplicity vs efficiency:** Google's desktop guidance says efficiency beats simplicity where people work for hours (keyboard shortcuts, density, multiple windows).

## 5. Sources

- Quality hub: https://developer.android.com/quality
- Excellent experiences: https://developer.android.com/quality/excellent
- Core value: https://developer.android.com/quality/core-value
- Material principles: https://m3.material.io/foundations/overview/principles
- Building with M3 Expressive: https://m3.material.io/blog/building-with-m3-expressive
- Expressive design research: https://design.google/library/expressive-material-design-google-research
- Desktop design principles: https://developer.android.com/design/ui/desktop
- Android 17 announcement (Compose primary, Views in maintenance mode): https://android-developers.googleblog.com/2026/06/Android-17.html
