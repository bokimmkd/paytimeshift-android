# PTS 0.2.12 (22) — Today and tomorrow widget

Based on 0.2.11 (21), commit a2508bf2ae4181f5908f89f57d2603e0ee69284c, premium-analytics-upgrade. Final application source: 61dbb3f6dda8c7709a458a8a73634dd9e697f34a.

- Rounded PTS widget with a small clock logo, separate Today / Tomorrow panels, job colors, times and paid duration in expanded sizes.
- Vertical and horizontal resizing: Android 12+ responsive sizes 240×128dp compact, 260×250dp expanded and 260×350dp with two rows per day. Earlier Android versions select layouts from widget options. Additional shifts use +N; taps open the selected shift or calendar day. Archived jobs are excluded. Overnight work stays on the start date with +1 beside its end time.
- Settings > Android widget offers only Activate widget when the launcher supports Android pinning. The launcher confirms addition. No instructions or extra first-launch promotion.
- Widget follows the app language, time format and appearance. Saved edits/restores refresh it; periodic and inexact next-day refresh update the dates. Android may delay background refresh while asleep.
- Existing Play update flow, optional Meta measurement and backup/billing rules retained.

## Validation

Android/backend workflow [37362293460](https://github.com/bokimmkd/paytimeshift-android/actions/runs/37362293460) passed: 109 debug + 109 release unit tests, 17 backend tests, debug/release lint, debug APK, signed AAB and CI jarsigner verification. UI workflow [37362293462](https://github.com/bokimmkd/paytimeshift-android/actions/runs/37362293462) passed all 33 instrumented tests with zero failures/errors/skips. Final compact/expanded/tall RemoteViews renders were visually checked in English/Macedonian and Light/Dark, including overnight, empty days and additional-shift visibility. Distinct shift targets and warm-launch calendar routing are covered.

Exact artifact: PTS-0.2.12-22.aab, 29,542,477 bytes.
SHA256: `6837298fa975dfd2345edb1d860e2d275c81653c1a76910e1b1787b5a1ad1c5c`.
CI artifact ZIP SHA256: `2fa746fdbd159dbeb9a1d31f0b74b1ac96c2f3c36a9219e7bef36f63e367d09f`.
Upload signer matches the existing certificate.

## Play submission — 5 October 2026

Google Play accepted code 22 / 0.2.12, minimum API 26 and target API 36. Existing closed-testing Alpha release 12 is named **0.2.12 · Today & tomorrow widget**, with 100% rollout and unchanged testers/countries. Submitted for review; publishing overview confirms **Changes in review**, quick checks running and managed publishing off. Approval and tester availability are pending. No Production rollout was performed.

Physical home-screen launcher placement, pinning, vertical resizing and day/shift taps remain tester checks; emulator rendering tests do not establish Samsung launcher behavior.
