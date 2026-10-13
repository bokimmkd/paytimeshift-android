# PTS 0.2.14 (24) — Alpha ready for review

Date: 2026-10-06, Europe/Skopje.

## Change and source

Continued from the latest premium-analytics-upgrade source, 04ffb7f32e985b3db3ec1ac02096122b811cd45f. This includes the selected-appearance system-bar fixes and restart/theme UI coverage.

Release source: 6d3395cbb330834cc80084afbfbdc8dd7f06a697.
Package: com.paytimeshift.pts. versionName 0.2.14; versionCode 24.

Status icons and clock use dark icons in Light mode and light icons in Dark mode. Window and navigation backgrounds follow the chosen PTS color scheme. Existing widget, update flow, billing, cloud and calculation behavior is retained.

## Validation and artifact

Android/backend workflow 37515413738 succeeded. Android job 112446957634: 111 debug and 111 release unit tests, no failures/errors/skips, lintDebug and lintRelease, signed AAB and jarsigner verification. Backend job 112446957875: 17 tests passed and generated language catalog check passed.

UI workflow 37515413764 / job 112446958738 succeeded: 39 tests, zero failures/errors/skips, including systemBarIconsFollowAppAppearanceAndSurviveRestart. Final emulator Light and Dark screenshots were inspected. A physical Samsung check is still required; emulator evidence is not a physical phone test.

Exact AAB: PTS-0.2.14-24.aab.
Size: 29,551,578 bytes.
SHA256: ef01af30013a62ba693f222564addfc63ba0319cba9c8524f006b2b815202626.
Actions artifact 11437093566 ZIP SHA256: 763c41f3eead0c95676879ecc473d3d67f20f946e323dfdb64458b759103298d.
Bundle manifest verified locally: com.paytimeshift.pts / 24 / 0.2.14.
ReTrace mapping embedded.
Existing upload certificate SHA1: 03:C2:26:1D:BF:1E:8E:65:99:41:15:06:32:9C:94:D8:AE:73:B8:BE.

## Actual Play state

Google Play accepted the bundle in the existing Closed testing Alpha track 4699298164505386169, release 14, named "0.2.14 · Light & Dark system bars", 100% full rollout. Saved successfully. No errors or lost supported devices; one nonblocking native-debug-symbol warning.

Publishing overview shows exactly one change, Alpha Start full rollout, under "Changes not yet submitted for review". Quick checks are running; managed publishing is off.

Submit for review was attempted but rejected by automatic approval review: the user's "finish here" continuation was judged insufficient explicit authorization for this exact external submission. The submission was NOT executed. Do not claim "Changes in review" or tester availability. Obtain explicit owner approval to submit this exact 24 / 0.2.14 Alpha release; do not route around the rejection.

Proof: PTS-0.2.14-Alpha-ready-1791314006864.jpg.

Fresh pre-upload Console evidence confirmed 0.2.13 (23) Available to testers. Production is Inactive and Apply for production disabled; 12 testers have been continuously opted in for 2 of the required 14 days. No Production access application or rollout was performed.

## Release notes

Improved readability of the phone clock, status icons and navigation buttons in Light and Dark mode. System bars now follow your selected PTS appearance, including after restarting the app.

## Next action

After explicit owner approval, submit the existing saved Alpha14 release from Publishing overview and verify its returned review state. Do not rebuild or upload again solely to submit. After availability, test 0.2.13 to 0.2.14 update and clock/icons on the owner's phone in Light/Dark/System, including app restart.

This release does not establish completion of the older automated report-email or real-device Meta attribution backlog.
