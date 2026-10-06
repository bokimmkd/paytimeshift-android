# PTS 0.2.8 (18)

Source: 1032424e26a860a5ba05ae88a648d337011f9b16 on premium-analytics-upgrade. Prepared and uploaded at the owner's request on 2026-10-04.

## Changes

- Cloud and manual backup restores keep the appearance selected on this phone (Light, Dark or System), including repeated restores and persisted local saves. Work data and other backed-up preferences are restored.
- An unbound phone with an existing cloud revision shows restore guidance instead of a generic connection error. Confirmed cloud restore offers a local backup file first; revision and overwrite guards remain.
- Pattern/import/calendar warnings show both literal job names and full shift intervals, including overnight dates, with a clear rest gap or overlap explanation. Warnings remain advisory.
- Calendar management supports deleting an inclusive shift-start-date range for a selected job or explicitly All jobs. Review count, leave/status scope and confirm/cancel precede the ordinary local save; stale reviewed selections are rejected. Other jobs, outside dates, salary and job settings are preserved.
- Earnings Pay period displays each job's covered dates even when empty. Existing payday and pay calculations are unchanged.
- New labels are localized in all nine supported languages.

## Validation

- Android/backend build run 37210828765 succeeded for source 1032424. Debug/release unit tests, lint, debug build, signed release bundle and CI jarsigner verification passed.
- Parsed XML: 92 debug and 92 release unit tests; zero failures, errors or skips. Restore regression covers Light/Dark/System, repeated restore, opposite-theme backup, restored jobs/preferences and serialized persistence.
- UI run 37210780258 succeeded for b2ac734ddf9c409ba6bcb359d2737b89e24c7d5b; its main/AndroidTest sources match the final source. The later commit changes only a unit-test currency fixture. Parsed XML: 22 UI tests, zero failures/errors/skips. Visually inspected cloud-restore-save-local, delete-shifts-review, pay-period-covered-dates and clear-shift-warning captures.
- Backend: 17 tests passed. Generated catalogs checked: 488 messages in each of eight translated catalogs plus English.
- Manifest decoded from the bundle: com.paytimeshift.pts / versionName 0.2.8 / versionCode 18.
- Upload certificate matches signing/pts-upload-certificate.pem: SHA-256 E1:58:94:C7:66:A9:04:40:5E:55:37:D8:4E:CD:33:3A:EF:F4:C3:35:58:78:0D:84:1A:31:49:A5:E0:25:29:F1.
- AAB 29,159,078 bytes; SHA-256 0e354c196aeb91928a15ca2e0ec1048eca280d7da03e491f50de2db8ccef5dc6. GitHub artifact 11306907589 archive SHA-256 9ba2b8544eb25caeafb239c62d2d05db045ecca2b46a1877b0915a13222d4f5a verified.
- Validation artifact 11306827604 archive SHA-256 e24e635b8ca1469aaf44c702a9e29d7e321bcc35cfb4dc69ad67139fed883d0c verified. UI artifact 11306627667.

## Owner account report and physical checks

Owner clarifies the backup issue and Light-to-Dark restore change occurred on the account with the permanent Premium grant; backup and restore work on other tester accounts. Server test access and Play subscriptions use the same backup/restore paths after access validation. Restoring the backed-up appearance explains the source behavior, but no device trace or live account database inspection proves the exact account-specific cause. No entitlement change or backend deployment is included.

Physical update validation remains open: keep a Play-installed 0.2.7 until 0.2.8 is available, then check the in-app offer, Later, download/restart and preserved data. CI does not prove a real Play download or the owner's live cloud restore. After installing, select Light, restore twice and restart to verify Light persists on the affected account.

## Play submission

- Uploaded the exact validated AAB to existing Closed testing - Alpha track 4699298164505386169, release 8, name "0.2.8 · Restore theme & schedule fixes", rollout 100%.
- Google accepted versionCode 18 / 0.2.8. No supported devices lost. The single native debug-symbol warning is nonblocking.
- Submitted exactly one change. Publishing overview confirms "Changes in review"; quick checks remain in progress. Managed publishing is off. Submission is confirmed; tester availability is not yet confirmed.
- Verified publishing URL: https://play.google.com/console/u/0/developers/4766816481672407684/app/4974514655440409843/publishing
- Saved proof: PTS-0.2.8-Play-review-1791126493448.jpg.
- No production rollout or backend deployment.
