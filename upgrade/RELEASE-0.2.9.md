# PTS 0.2.9 / versionCode 19

Source: ac4911a3c188b9f59665dd75b767956dd41b149b on premium-analytics-upgrade.
Package: com.paytimeshift.pts. Scope: Calendar menu clarity and optional Google Play update feedback.

## Changes

- Calendar overflow order: Import roster, Share schedule, Mark/Unmark holiday with the selected localized date, divider, Delete shifts in a period. All actions have aligned leading icons; deletion uses the theme error color in Light and Dark. The holiday date appears below the label to keep long translations readable.
- Non-modal Waiting/Downloading/Installing card. Download percentage uses Play's byte counts when a positive total is available; otherwise progress is indeterminate. Normal app use remains available while downloading.
- Ready requires Play DOWNLOADED, including instant/cached downloads; reaching 100% alone does not offer Restart. Session/query guards reject late responses; installed versions cannot revive old offers. Restart is guarded against repeated taps; completeUpdate failure restores Ready with a localized retry explanation.
- Existing consent, Later/version deferrals, optional updates and Store route retained. No changes to work/pay calculations, backup deduplication/overwrite guards, billing or account grants. No live backend deployment required for these Android UI changes.

## Validation

Final Android/backend run: https://github.com/bokimmkd/paytimeshift-android/actions/runs/37218567722
Final UI run: https://github.com/bokimmkd/paytimeshift-android/actions/runs/37218567732

- 99 debug and 99 release unit tests: zero failures/errors/skips; includes slow/instant transfers, resumed progress, 100%-without-DOWNLOADED, installation retry/repeated Restart, late/obsolete/installed events and existing calculations/restore/deletion regressions.
- 25 instrumented tests: zero failures/errors/skips. Calendar selected-date mark/unmark/order/deletion-scope callbacks tested in Light and Dark; download UI preserves a working normal app action, installing hides Restart, and retry explanation is localized. Existing backup/restore, hints, warnings and reports checks also pass.
- Debug/release lint, debug build, signed bundle and CI jarsigner verification pass. 17 backend tests pass; 494 messages generated across eight translated languages plus English default.
- Artifact ZIP digests verified locally before extraction. UI screenshots reviewed for both Calendar themes, downloading, installing and installation retry.

## Bundle identity

- Filename: PTS-0.2.9-19.aab
- Size: 29,170,412 bytes
- AAB SHA-256: 0524502cb82b67994638ea5c624a422cecf2e593050f634a691eb0f7ae006df4
- Decoded manifest: com.paytimeshift.pts, versionCode 19, versionName 0.2.9.
- Upload certificate SHA-256: E1:58:94:C7:66:A9:04:40:5E:55:37:D8:4E:CD:33:3A:EF:F4:C3:35:58:78:0D:84:1A:31:49:A5:E0:25:29:F1; matches existing upload certificate.
- AAB artifact: 11308993705; ZIP SHA-256 d565fab3b51218774bb88eaa003b667708dd883606a28b7391da9352caa9f090.
- Unit/lint artifact: 11309371386; ZIP SHA-256 82e28293553326c995ea9fccb49bdeff582a1213d029a8e503c5d515bf016782.
- UI artifact: 11309316136; ZIP SHA-256 74e24ddbde19ab868a80ffe5193885f8a96065d785f6e3952e7ddd6926fa80ee.

## Physical acceptance checks

- After Play installation, About must show 0.2.9; existing shifts, jobs, Premium and theme must remain.
- Calendar: confirm Import/Share/Holiday/Delete order, selected date, mark/unmark and deletion review/cancel. Check both appearance modes and the user's selected language.
- Cloud backup case is closed by owner's 18:13 confirmation: editing data then backing up works; unchanged-data deduplication keeps the old timestamp. Retain a smoke check of changed-data backup and theme-preserving restore.
- A 0.2.8 → 0.2.9 flexible update is executed by the updater in 0.2.8. The new progress/retry UI in 0.2.9 needs a subsequent higher Play version for real-device integration validation; emulator/unit checks alone do not establish Play eligibility, network timing or native restart behavior.
- Treat this as a release candidate pending physical acceptance, not production approval.

## Google Play

Submitted 2026-10-04 at approximately 19:10 Europe/Skopje to Closed testing – Alpha, track 4699298164505386169, release 9, name 0.2.9 · Calendar & update polish. Only version 19 is included; previous version 18 is excluded. Rollout is 100% of the existing Alpha tester audience. No loss of supported devices. One nonblocking native debug-symbol warning remains.

Publishing overview confirms Changes in review, managed publishing off and quick checks in progress (up to 13 minutes shown). Tester availability and Google approval are pending; no production rollout is claimed.

Verified publishing URL: https://play.google.com/console/u/0/developers/4766816481672407684/app/4974514655440409843/publishing
Proof: PTS-0.2.9-Play-review-1791133836264.jpg.
