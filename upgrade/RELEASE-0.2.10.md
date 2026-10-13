# PTS 0.2.10 / versionCode 20

Source: a45d5062bd580e3797683644a5a028f2e9c0e060 on premium-analytics-upgrade.
Package: com.paytimeshift.pts. Scope: owner-defined optional Google Play update flow.

## Behavior

- PTS offers Update / Later. Update opens Google's own flexible-update consent screen.
- After Play consent, normal PTS use continues with a non-modal Waiting/Downloading indicator. No new PTS update popup appears while downloading. A 100% byte count does not by itself permit Restart.
- Actual Play DOWNLOADED offers Restart / Later. Restart calls completeUpdate once, hides the prompt and shows Installing while Play handles native installation/restart. Failure restores Ready with the existing retry explanation.
- Later on either offer suppresses only that version for the current app visit. A fresh launch or full background/foreground transition offers it again; rotation, transient pause/resume and the Play consent sheet keep the same visit. Previous persisted 24-hour deferrals are no longer used.
- Session, active consent, version and transfer state survive activity recreation using the activity ViewModel. Old update queries and installed-version events remain rejected.
- No Calendar, work/pay, backup, billing or account-grant changes.

## Physical evidence and next acceptance

Owner reports Calendar acceptance and successful native installation/opening for 0.2.8 → 0.2.9, but the old PTS Update dialog reappeared briefly between Play consent and Ready. No measured network/cache timing or explicit About/version confirmation is inferred.

- On a Play-installed tester phone, update from 0.2.9 to 0.2.10: accept PTS Update, then Play Update; verify the original PTS popup stays hidden while downloading, and Restart appears only when Play says the download is ready. Tap Restart and confirm native installation, About 0.2.10 and preserved data/theme.
- Updating into 0.2.10 runs the updater in installed 0.2.9. The changed next-opening Later policy and retained state added in 0.2.10 require running 0.2.10 with a subsequent higher Play version available for physical integration acceptance. Automated tests establish the policy/lifecycle behavior, not live Play eligibility or device download timing.
- Check Later on both Available and Ready, repeated polling in the same visit, leave/reopen PTS, rotation during consent/download, slow and fast downloads, cancel consent and installation failure/retry.

## Validation and bundle

Android/backend run: https://github.com/bokimmkd/paytimeshift-android/actions/runs/37224216796
UI run: https://github.com/bokimmkd/paytimeshift-android/actions/runs/37224216807

- 101 debug + 101 release unit tests; zero failures/errors/skips. Includes foreground-only deferral, slow/instant download, consent-before/after-Ready, 100%-without-DOWNLOADED, installed-event suppression and installation retry.
- 27 instrumented tests; zero failures/errors/skips. Both new activity lifecycle tests pass: Later survives rotation and clears after leave/reopen; active Play consent survives rotation without reviving Available.
- Debug/release lint, debug build, release bundle, CI jarsigner verification and backend checks pass. 17 backend tests pass. Generated translations remain consistent.
- Artifact ZIP SHA-256 digests checked before extraction. Decoded bundle manifest verifies com.paytimeshift.pts, code 20, name 0.2.10.
- AAB filename: PTS-0.2.10-20.aab; size: 29,171,755 bytes.
- AAB SHA-256: 0abfa5fc3eabc8784672937b05ef1bf630f01e22699edbdc7271476b140923b8.
- Upload certificate SHA-256: E1:58:94:C7:66:A9:04:40:5E:55:37:D8:4E:CD:33:3A:EF:F4:C3:35:58:78:0D:84:1A:31:49:A5:E0:25:29:F1; matches existing upload certificate.
- AAB artifact 11311811016, ZIP digest fabc041c5d70c00a1320b4dad87a9bab502168d598ddbb5e7fdf07b112a835ba.
- Unit/lint artifact 11311024724, ZIP digest 33e2da6c7d8964146b285f24a3edc3d920e91a89b1714e61b42b46bc724b505c.
- UI artifact 11310994716, ZIP digest b14379c872c5b6ea60d5e6904c97c6b7c04f8938cca5c5790be1bf12531c5b4e.

## Google Play

Submitted 2026-10-04 at approximately 20:37 Europe/Skopje to Closed testing – Alpha, track 4699298164505386169, release 10, name 0.2.10 · Play update flow. Only version 20 is included; previous version 19 is excluded. Rollout is 100% of the existing Alpha tester audience. No loss of supported devices; one nonblocking native debug-symbol warning remains.

Publishing overview confirms Changes in review, managed publishing off and quick checks running (up to 13 minutes shown). Approval/tester availability remain pending. No production rollout or physical acceptance of the new updater is claimed.

Verified URL: https://play.google.com/console/u/0/developers/4766816481672407684/app/4974514655440409843/publishing
Proof: PTS-0.2.10-Play-review-1791139028837.jpg.
