# PTS 0.2.5 (15)

Base: premium-analytics-upgrade, 2f5d2a4d542a931e81b94db0ba864129b2130e47 (0.2.4 / 14).

## Android changes

- Calendar retains job markers for Off, Vacation, Sick and Non-working day with a red minus. Work counts exclude these entries. Existing zero hourly/daily earnings and unchanged fixed monthly salaries are preserved.
- A separate small exclamation mark flags insufficient rest. Structured warnings share one calculation with day details, including other jobs, overnight shifts and month boundaries.
- Rotation day inputs can be cleared and edited; stable row identities preserve input/focus on add/remove. Preview rejects blank, zero and out-of-range counts with localized feedback.
- Account refresh is coalesced, retries the server's status rate limit once, runs on foreground/account opening, and preserves same-user entitlement during refresh. An unresolved/error status is distinct from Free. Cloud actions require resolved Premium; a local binding no longer claims an uploaded backup. Duplicate in-flight purchase restoration/verification is coalesced.
- Settings and local-backup buttons have compact visible heights/padding while retaining Material touch targets. Support email is visible and opens an email draft; when no email app exists the address is copied.
- Website: https://paytimeshift.com/; current privacy policy: https://paytimeshift.com/privacy-premium.html; support: bokimk.ap@gmail.com. The older privacy.html covers 0.1.7 and is not the current app policy.
- New messages are translated into all eight supported translated languages; English remains the default. The displayed build version is read from BuildConfig.

## Validation and remaining integration work

Build and UI workflow results, signed artifact checksum and final source commit must be recorded after completion. Existing reminder/pay/update tests remain part of release validation. Added regressions cover absence marker/pay behavior, short-rest thresholds/month boundaries, cloud status distinctions, editable rotation counts, enabled Premium cloud controls and calendar marker coexistence.

The owner confirms website publication. Retrieval from the build session still returns 502/inaccessible, so public page opening remains a separate device/public check; the paths are verified in the supplied website archive.

The Android refresh/UI fixes do not prove the cause of the owner's live entitlement mismatch or a successful cloud upload. Test the actual Premium account, backup/restore including new salary/adjustment/status fields, real Play license purchase/acknowledgement/restore and the 0.2.4 -> 0.2.5 update offer on a physical phone.

RTDN remains incomplete. Resend/report email deployment remains deferred. Meta integration remains unimplemented pending a confirmed PTS-specific Meta App ID, account linking and clean-install event verification. No client-side email-based Premium override, backend deployment or advertising campaign activation is included.

A signed AAB is a build artifact, not evidence of a Play upload or rollout. Production publication is not authorized by this task.
