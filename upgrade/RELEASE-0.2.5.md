# PTS 0.2.5 (15)

Base: premium-analytics-upgrade, 2f5d2a4d542a931e81b94db0ba864129b2130e47 (0.2.4 / 14).

## Android changes

- Calendar retains job markers for Off, Vacation, Sick and Non-working day with a red minus on a small white background that stays visible on red job colors. Work counts exclude these entries. Existing zero hourly/daily earnings and unchanged fixed monthly salaries are preserved.
- A separate small exclamation mark flags insufficient rest. Structured warnings share one calculation with day details, including other jobs, overnight shifts and month boundaries.
- Rotation day inputs can be cleared and edited; stable row identities preserve input/focus on add/remove. Preview rejects blank, zero and out-of-range counts with localized feedback.
- Account refresh is coalesced, retries the server's status rate limit once, runs on foreground/account opening, and preserves same-user entitlement during refresh. An unresolved/error status is distinct from Free. Cloud actions require resolved Premium; a local binding no longer claims an uploaded backup. Duplicate in-flight purchase restoration/verification is coalesced.
- Settings and local-backup buttons have compact visible heights/padding while retaining Material touch targets. Support email is visible and opens an email draft; when no email app exists the address is copied.
- Website: https://paytimeshift.com/; current privacy policy: https://paytimeshift.com/privacy-premium.html; support: bokimk.ap@gmail.com. The older privacy.html covers 0.1.7 and is not the current app policy.
- New messages are translated into all eight supported translated languages; English remains the default. The displayed build version is read from BuildConfig.

## Validation and remaining integration work

Final Android source commit: eaf038ab23751d11e6a37a2aa3968e4a3a26e7b8. Build workflow 37192622764 and UI workflow 37192622779 both completed successfully. Existing reminder/pay/update tests remain part of release validation. Added regressions cover absence marker/pay behavior, short-rest thresholds/month boundaries, cloud status distinctions, editable rotation counts, enabled Premium cloud controls and calendar marker coexistence.

The owner confirms website publication. Retrieval from the build session still returns 502/inaccessible, so public page opening remains a separate device/public check; the paths are verified in the supplied website archive.

The Android refresh/UI fixes do not prove the cause of the owner's live entitlement mismatch or a successful cloud upload. Test the actual Premium account, backup/restore including new salary/adjustment/status fields, real Play license purchase/acknowledgement/restore and the 0.2.4 -> 0.2.5 update offer on a physical phone.

RTDN remains incomplete. Resend/report email deployment remains deferred. Meta integration remains unimplemented pending a confirmed PTS-specific Meta App ID, account linking and clean-install event verification. No client-side email-based Premium override, backend deployment or advertising campaign activation is included.

A signed AAB is a build artifact, not evidence of a Play upload or rollout. Production publication is not authorized by this task.


## Final signed artifact and evidence

- AAB: PTS-0.2.5-15.aab, 29,129,748 bytes. Manifest verified as com.paytimeshift.pts, versionName 0.2.5, versionCode 15.
- SHA-256: 49b29ce9d68ee674c718d9130c0d14c1862a9ae96e1ab57b46a62774daa000b3
- GitHub signed bundle artifact: 11299293523 (PTS-Play-AAB), built from the final source above. Downloaded ZIP digest matches GitHub's recorded SHA-256.
- CI jarsigner verification passed. The AAB signing certificate matches signing/pts-upload-certificate.pem; SHA-256 E1:58:94:C7:66:A9:04:40:5E:55:37:D8:4E:CD:33:3A:EF:F4:C3:35:58:78:0D:84:1A:31:49:A5:E0:25:29:F1. Local bundletool 1.18.3 validation passed.
- Android unit tests: 73 debug and 73 release; zero failures, errors or skipped tests. Debug and release lint/build checks passed.
- Instrumented UI tests: 17; zero failures, errors or skipped tests. The final screenshot was inspected: the red-minus marker stays distinct on a red-colored job and the short-rest exclamation mark has its own corner.
- Backend tests: 17 passed, zero failures. Generated language catalogs checked cleanly (476 messages, eight translated languages).
- The signed AAB was saved and delivered before the owner subsequently authorized the closed-testing submission below. No production publication or backend deployment was performed.


## Owner-authorized Play submission — 2026-10-04

- Owner instruction: publish the delivered AAB. Google session was reauthenticated using the secure browser credential flow.
- Uploaded the exact verified PTS-0.2.5-15.aab (SHA-256 49b29ce9d68ee674c718d9130c0d14c1862a9ae96e1ab57b46a62774daa000b3).
- Google accepted version code 15 / versionName 0.2.5. Existing Closed testing - Alpha, track 4699298164505386169, release 5, name `0.2.5 · Calendar & account fixes`, rollout 100%.
- Reviewed the single non-blocking native debug-symbol warning. No supported phone/tablet devices were lost. Submitted only this one release change for review.
- Publishing overview visibly shows `Changes in review` with `Running quick checks for commonly found issues`; changes go onward for review after successful quick checks. Managed publishing remains off. This is submission confirmation, not confirmation that 0.2.5 is already available to testers.
- Publishing URL: https://play.google.com/console/u/0/developers/4766816481672407684/app/4974514655440409843/publishing
- Saved proof: PTS-0.2.5-Play-review-1791108373483.jpg.
- Google approval/availability and actual device update/payment/cloud tests remain pending. Resend, RTDN and Meta integration are unchanged.
