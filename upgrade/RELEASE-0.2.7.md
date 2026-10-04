# PTS 0.2.7 (17) release candidate

Prepared at the owner's request on 2026-10-04. Source: 093648439db27caf0cb0c529f916de52389f5ac6 on premium-analytics-upgrade. Carries all 0.2.6 changes.

## Changes

- Add job now uses the app's turquoise background (#00857C), white icon/text and Material button behavior. Visible height remains 44 dp.
- Play update checks run on resume and retry while the app remains in the foreground (first retry after 15 seconds, then every 30 seconds).
- A single completion callback and request/session tickets avoid a paused, timed-out or older request blocking or overwriting a newer check.
- An available update without an allowed flexible flow offers a localized Open Google Play action. Failed flow launches/results and download failures can use the same route; a failed restart offers Restart again.
- Starting a download does not write a 24-hour postponement. Explicit Later/cancel postpones that version; restart postponement is also per version.
- Preserve a newer downloaded/Ready event against an older query or consent result. Avoid offering another download during active download/install.
- Added two messages to all eight generated language catalogs; generation is clean (478 messages per language).

## Validation

- Build workflow 37202260071: success; Android job 111436193625 and backend job 111436193754.
- UI workflow 37202260083: success; job 111436193318.
- Parsed XML: 82 debug + 82 release unit tests and 18 instrumented UI tests, zero failures/errors/skips.
- Backend: 17 tests passed, zero failures. Debug/release lint and builds passed; CI jarsigner reports jar verified.
- Local bundletool 1.18.3 validation passed. Manifest: com.paytimeshift.pts / versionName 0.2.7 / versionCode 17.
- Certificate matches signing/pts-upload-certificate.pem: SHA-256 E1:58:94:C7:66:A9:04:40:5E:55:37:D8:4E:CD:33:3A:EF:F4:C3:35:58:78:0D:84:1A:31:49:A5:E0:25:29:F1.
- Visually inspected jobs-turquoise-add-button.png (empty-install Jobs screen) and update-play-store-fallback.png (Macedonian Store-action dialog). Labels fit and the turquoise action is visible.
- Existing launcher/navigation test captures the styling evidence. New UI test verifies localized Store text and its confirm callback; it does not launch an external Store or prove a device Play response.
- Diff check passed; no backend deployment.

## Signed artifact

- PTS-0.2.7-17.aab: 29,130,338 bytes.
- SHA-256: 4725317aa5766d2889470b9c6cd8d449868668ea07a2a3aa82bd8a9132cb221c.
- GitHub AAB artifact 11303696236; archive SHA-256 39b83ef0067cffff70a6574f2f8b9d56719a6833eacf03808a11e1f158fe663e verified.
- Unit validation artifact 11302799393; archive SHA-256 085fe241d91fec5e973269af65497d0a352248b79b53459f45a157f4c7352b28 verified.
- UI artifact 11303770780; archive SHA-256 38f83eb1bcebda5a220fe0903511560753c3bb96ba100bbb06c4e6f64a147ba0 verified.
- Signed AAB saved for delivery.

## Phone report and release status

The owner confirmed installed 0.2.5 earlier. Alpha 0.2.6 is available to selected testers. Screenshot 1791116039853.jpeg shows Update on the phone's Play listing, yet the owner reports no PTS offer after returning to the app. The device's appUpdateInfo response and flexible-flow permission were not observed, so the exact cause is not established.

This 0.2.7 is prepared and validated, not uploaded or published. Current published Alpha remains 0.2.6 / 16. The old installed client cannot receive this new checking logic until 0.2.7 is installed. Physical end-to-end validation of the improved offer needs an installed 0.2.7 followed by a higher-version Play release; CI is not proof of a real Play download/restart.

Physical payment/purchase restore and owner-account cloud backup/restore remain open integration checks. RTDN, deferred paid-mail/report sending and Meta integration are unchanged.
