# PTS 0.2.11 / 21 — Meta install measurement

Base: premium-analytics-upgrade at 663686217d84edd38425cec5a0575426ef21cc3c; application baseline 0.2.10 / 20 at a45d5062bd580e3797683644a5a028f2e9c0e060. Owner supplied PTS Meta App ID 1899290267897887 and authorized a new AAB on 2026-10-04, superseding the previous testing hold.

Source: 88667e4f84e2c1dd56a231b9cd96c290a9da1312. Package com.paytimeshift.pts; launcher com.paytimeshift.pts.MainActivity. Fixed dependency com.facebook.android:facebook-core:18.3.0, verified against official SDK source and publisher Maven metadata. Includes its public client token, not an App Secret or administrative access token.

## Behavior

- A non-modal, localized App measurement card offers Allow / Not now. The phone-specific setting starts off and can be changed in Settings, independently of Premium. It is separate from AppData and all backup/restore payloads.
- The SDK init provider is removed. Declining or never choosing leaves Meta uninitialized. PTS manually queues install and activation after opt-in, with a 30-second background session boundary; rotation and short Play/Billing visits do not duplicate PTS activations.
- Manifest and runtime disable automatic event logging and Meta advertising ID collection. Flush is explicit. PTS passes no job, shift, pay, note, account ID/email or purchase parameters. LimitEventAndDataUsage restricts event use to analytics/conversions.
- Optional vendor measurement does not block app startup or user data if SDK initialization fails. SDK networking and install deduplication use SDK behavior; enqueue success is not server/Events Manager proof.
- Calendar, backup, billing, work/pay calculations and the accepted Play update controller are unchanged.

## Meta Android registration

Android Google Play platform saved and verified after reload in the PTS Meta app. Package and launcher match source; automatic purchase logging is off. Key hashes:

| Channel | Meta hash |
| --- | --- |
| Debug (checked from committed debug certificate) | suvgjvxKl65mUth+ckcrL2DTxmU= |
| Upload (checked from existing upload certificate) | A8ImHb8ejmWZQRUGMpyU2K5zuL4= |
| Play App Signing (retained previously verified PTS setup record) | vPz+naheVSd+QQusu4NUxkXQ20A= |

Play signing SHA-1: BC:FC:FE:9D:A8:5E:55:27:7E:41:0B:AC:BB:83:54:C6:45:D0:DB:40. Fresh Console certificate inspection was blocked by expired Google sign-in; no key rotation is claimed. Meta app currently unpublished. Its observed Business Portfolio context is 3881159028598226. No ad account authorization, campaign publication/spend or real-device Events Manager success is claimed.

## Privacy and remaining acceptance

Optional Meta install/app-open disclosure is published and verified on https://paytimeshift.com/privacy-premium.html, updated 5 October 2026. The nine-language homepage/layout were preserved. Google Pages deployment and website validation passed. Console Privacy Policy and account deletion URLs are aligned with paytimeshift.com and submitted with this Alpha release. Existing live Data Safety collection/sharing of app interactions, device IDs, approximate location, diagnostics and other performance data for analytics/fraud prevention/advertising was inspected; collection categories and purposes were not changed. Public policy proof: PTS-0.2.11-Public-Meta-policy-1791154089316.jpg.

Install the new build through Play on a real test phone, choose Allow and verify install/activation in Events Manager for 1899290267897887. Verify Not now, Settings off/on, restart and rotation, and confirm no measurement choice is inherited by backup restore. Test both Free and Premium without changing entitlement/account data. Do not clear or uninstall a user's only saved shifts to force a clean install; use a spare tester/device or preserve data first. Verify ad-account linking and Meta publication requirements before advertising activation; no spend is authorized by this AAB request.

## Validation

Android/backend run 37236690447 and instrumented UI run 37236690571 pass. 106 debug + 106 release unit tests, 29 instrumented tests, and 17 backend tests pass with zero failures/errors/skips. Lint and CI jarsigner verification pass. Artifact ZIP digests match publisher metadata. Final AAB manifest confirms com.paytimeshift.pts / code 21 / 0.2.11, and the upload certificate matches the existing signer. Five new unit tests cover consent, rotation/session deduplication, withdrawal/regrant and retry; two new instrumented tests check PTS metadata and absence of automatic initialization.

AAB: PTS-0.2.11-21.aab, 29,522,552 bytes. SHA-256 889c74bafdd27fe59a06986517896a896b52fb1e8876f8e95e689957a5017347. Upload signer SHA-256 E1:58:94:C7:66:A9:04:40:5E:55:37:D8:4E:CD:33:3A:EF:F4:C3:35:58:78:0D:84:1A:31:49:A5:E0:25:29:F1.

Owner explicitly authorized public policy publication and Alpha submission on 2026-10-05 Europe/Skopje. Live paytimeshift.com policy now includes the optional Meta disclosure and updated date. GitHub Pages main/docs commit cda4963bf13042ee983b3946f8222bbb4ee593f4 deployed successfully in run 37241265786; PTS website validation run 37241266206 passed. Console Privacy Policy and account deletion URLs now use https://paytimeshift.com/privacy-premium.html and https://paytimeshift.com/delete-account.html. Data Safety collection/sharing types and purposes were read back; only the deletion URL was changed in that questionnaire. Exact previously validated AAB 21 / 0.2.11 is submitted as Alpha release 11, 100% full rollout, alongside the two declarations. Publishing overview confirms Changes in review, quick checks running, managed publishing off. Google approval and tester availability are pending. No production launch, real-device Meta event, campaign or spend is claimed. Play proof: PTS-0.2.11-Play-review-1791154378837.jpg. Meta setup proof: PTS-0.2.11-Meta-Android-1791150044876.jpg.
