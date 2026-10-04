# PTS 0.2.6 (16) release candidate

Prepared at the owner's request on 2026-10-04. This candidate carries all 0.2.5 changes and reduces the two remaining large Account & Premium buttons: Restore purchases and Sign out. Both now match Refresh account and cloud controls (36 dp minimum visible height, 8 dp horizontal / 3 dp vertical content padding). Existing Material touch targets, labels, icons and click behavior are preserved.

Source: af338cdf07f58bbb08225ac8595d8a84a73d8a28 on premium-analytics-upgrade. PR #2 is included and GitHub marks it merged. The only application changes relative to published 0.2.5 are these two button dimensions and the version increment to 0.2.6 / 16.

## Validation

- Build workflow 37198005522: success; Android job 111423757724 and backend job 111423757540.
- UI workflow 37198005511: success; job 111423757402.
- Unit tests: 73 debug + 73 release, zero failures/errors/skips. Debug and release lint/build checks passed.
- Instrumented UI: 17 tests, zero failures/errors/skips. Restore purchases is visibly compact in the existing free-account screenshot; no new signed-in screenshot was added for this styling-only patch.
- Backend: 17 tests passed, zero failures; generated catalogs checked cleanly.
- CI jarsigner reports jar verified. Local bundletool 1.18.3 validation passed; manifest verified as com.paytimeshift.pts / versionName 0.2.6 / versionCode 16.
- AAB certificate matches signing/pts-upload-certificate.pem: SHA-256 E1:58:94:C7:66:A9:04:40:5E:55:37:D8:4E:CD:33:3A:EF:F4:C3:35:58:78:0D:84:1A:31:49:A5:E0:25:29:F1.
- Diff check passed. Source scope and existing click handlers are unchanged except button layout and build version.

## Signed artifact

- File: PTS-0.2.6-16.aab; 29,129,632 bytes.
- SHA-256: 7908710c56beac49aaa2d8e470100ccf387aa750aae063a7262e6933fc5bbedb.
- GitHub artifact 11301697169, PTS-Play-AAB; archive SHA-256 9b1026a1ef79d6412b062298f46a5f3f684235c010c541da4e010a0fee99573f verified after download.
- Unit validation artifact 11302510537 and UI artifact 11302016079 were downloaded and their GitHub archive digests verified. Test totals above were parsed from their XML results.
- Signed AAB saved for delivery.

## Release status and remaining device checks

This is an Android build candidate, not a Play publication. Published Alpha remains 0.2.5 / 15. No new Play rollout or backend deployment was performed.

The owner's installed version and flexible-update download/restart completion still need physical-phone confirmation. Payment/restore and actual cloud backup/restore remain integration checks from RELEASE-0.2.5.md; RTDN, deferred report email and Meta integration are unchanged. Do not claim those integrations are verified or that production is ready solely from this candidate's CI result.


## Owner-authorized Alpha submission — 2026-10-04

- Owner confirms Settings shows 0.2.5 on the phone. Installed-version verification is now complete for the 0.2.4 -> 0.2.5 update report. The exact download/restart sequence was not separately observed; do not continue classifying installation as unconfirmed.
- Owner explicitly requests publication of 0.2.6. Uploaded the exact validated PTS-0.2.6-16.aab, SHA-256 7908710c56beac49aaa2d8e470100ccf387aa750aae063a7262e6933fc5bbedb.
- Google accepted versionCode 16 / 0.2.6. Existing Closed testing - Alpha track 4699298164505386169, release 6, name "0.2.6 · Compact account buttons", rollout 100%.
- Reviewed the single nonblocking native debug-symbol warning. No supported devices were lost. Submitted exactly one release change for Google review.
- Publishing overview positively shows "Changes in review" for this release; quick checks are still running (up to 13 minutes shown in proof). Managed publishing is off. Submission is confirmed; availability to testers is not yet confirmed.
- Publishing URL: https://play.google.com/console/u/0/developers/4766816481672407684/app/4974514655440409843/publishing
- Saved proof: PTS-0.2.6-Play-review-1791113363940.jpg.
- No production publication or backend deployment. Payment/cloud/RTDN/report email/Meta items remain as recorded above.
