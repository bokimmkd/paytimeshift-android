# PTS — next update requirements

Recorded: 2026-10-03. These are implementation requirements, not a claim that all features below are released.

## 1. Change the status of a scheduled day (implemented and CI-validated in 0.2.4; live deployment pending)

- In Add Shift / Edit Shift, support Work, Off, Vacation, Sick, and Non-working day.
- A non-working status must be selectable even when that job already has shifts on the date. Do not reject it with "This shift already exists".
- Before replacing existing entries, list the affected entries and ask for explicit confirmation. Cancel must leave data untouched.
- Apply the change only to the selected job and selected shift-start date. If two jobs share the date, either job can be changed independently; never change the other job.
- A full-day non-working status replaces all shifts for the selected job/date. Editing one shift into that status must also review any additional shifts for that same job/date.
- Changing a non-working status back to Work must explicitly replace the status entry instead of leaving contradictory work and absence records.
- Hide time, break and bonus inputs for non-working statuses. Existing Off/Vacation/Sick behavior remains zero estimated pay for hourly/daily jobs; paid leave rules are a separate future decision.
- Preserve local storage, manual/cloud backup, colleague schedule export/import, translations, reminders and analytics compatibility.
- "Non-working day" is a job-specific calendar status; it is distinct from the global Holiday flag used for holiday pay on worked shifts.

## 2. Fixed monthly salary per job (implemented and CI-validated in 0.2.4; live deployment pending)

- Add a pay basis selector: Hourly, Per shift / daily, Fixed monthly salary.
- Monthly amount and currency belong to each job independently.
- The base monthly estimate is the entered salary for the entire calendar month: it does not change with 28, 29, 30 or 31 days, number of scheduled shifts, or number of scheduled working days.
- Monthly base must be counted once per job and month, never once per shift. Example: a monthly salary of MKD 40,000 remains MKD 40,000 in February, April and October.
- Work hours and attendance statistics continue to come from actual saved shifts. A non-working status does not silently prorate or reduce a fixed monthly base.
- Keep salary month and payday/payment period distinct.
- Specify and test partial-month, employment start/end, archived-job and weekly/payment-period allocation behavior before implementation; do not invent salary for months before a job starts or after it ends.
- Keep separately configured overtime/night/weekend/holiday extras explicit; do not interpret the monthly salary as an hourly rate or apply the existing per-shift fixedPay flag.
- Work costs are subtracted separately after gross estimated earnings, as before.
- Never merge currencies. Avoid division by zero when hours are zero.
- Preserve existing hourly/per-shift jobs and saved rate snapshots. Android, server analytics, PDF, monthly/yearly reports and backup must agree.
- Verify equal monthly bases across different month lengths, no duplicate salary when adding shifts, job independence, and unchanged legacy pay calculations.

## 3. Cross-job overlap warning (implemented in source, not newly published to Play)

- Source commit: ad5f2b80ea468be8115c9bc446c58d8459e63937.
- Before saving an overlapping manual work shift, show all overlapping jobs/times with Edit shift / Save anyway.
- Exclude the edited entry itself and non-working statuses; handle overnight intervals and allow touching endpoints.
- Android unit/UI validation and build workflows passed for that commit.

## 4. Monthly deductions and bonuses per job (implemented and CI-validated in 0.2.4; live deployment pending)

- Add a compact Monthly adjustments section in Earnings for the selected month, grouped by job. Support hourly, daily/per-shift and fixed-monthly jobs.
- Add deduction / Add monthly bonus: positive amount, job currency, reason/category and optional note. Support multiple entries and editing/removing individual entries.
- Deduction reasons: absence, sick leave, unpaid leave, lateness, other penalty, and a custom reason. Monthly bonus has its own reason/description.
- Each entry belongs to exactly one job and one YYYY-MM month. It must not affect another job, adjacent month or recur automatically.
- Show separate lines for gross estimated earnings, monthly bonuses, monthly deductions, adjusted estimated earnings, work costs and estimated real earnings.
- Calculation: adjusted estimated earnings = existing gross estimate + monthly bonuses - monthly deductions. Estimated real earnings = adjusted estimated earnings - work-related costs.
- Keep the existing gross pay engine and saved per-shift bonuses unchanged; monthly adjustments are a separate layer and must never be counted once per shift or twice in monthly/yearly totals.
- Monthly deductions are manually entered. Marking absence/sick/unpaid leave must not invent an automatic penalty or double-charge lost hourly/daily earnings. Fixed monthly salary stays unchanged until a manual deduction is entered.
- Work costs remain separate from payroll adjustments. Do not automatically calculate taxes or government deductions, and do not label the result official net salary.
- Preserve currency snapshots and show separate currency totals. Avoid division by zero in effective hourly values.
- Include individual adjustment details and totals in monthly/yearly analysis and branded PDF/email reports.
- Persist locally, include in manual and cloud backups, and maintain backward compatibility: missing adjustments mean an empty list. Keep Android/server calculations aligned.
- Validate with examples including multiple jobs in one month, changing months, fixed monthly salary across month lengths, combined deductions and bonuses, legacy per-shift bonuses, separate work costs, and different currencies.

## 5. Feature placement and localized dismissible hints (implemented and CI-validated in 0.2.4; live deployment pending)

- Add/Edit Job: a compact Pay basis selector in the existing Pay section. Show the amount input for Hourly, Per shift/daily, or Fixed monthly salary according to the chosen basis.
- Add/Edit Shift: use the existing Day type selector for work/absence status. Present the job/date-specific replacement confirmation only when saving affects existing entries.
- Earnings: under each job's selected-month summary, add a compact Monthly adjustments entry. Its editor lists deductions and bonuses with small icon buttons for adding/editing/removing entries. Keep the main tab compact.
- Reports: display adjustment details and separate summary lines in the existing branded report layout.

### Contextual help

- Add short, branded field-anchored tooltips, like Excel input help, at key fields: pay basis, monthly salary, overtime/weekend/holiday rates, paid break, minimum rest gap, day type, pattern replacement, monthly adjustments, and work costs.
- Show a tooltip only when the user taps or focuses its field; do not open hints automatically on first use or when opening a screen. Keep normal typing, date/time pickers and dropdown actions working on the same interaction.
- Tooltips explain the field in plain language and appear beside their field, rather than as permanent cards below it. Do not create a blocking tutorial.
- Each visible bubble has a small X to dismiss that individual hint. Keep a usable touch target despite the compact visual X.
- Add one Settings toggle, Show hints, to hide all help bubbles globally. Save both the global setting and per-hint dismissal locally across app restarts and upgrades.
- Use stable semantic hint IDs; changing language must not reset dismissals or couple the preference to translated strings.
- Hint content and controls use the currently selected app language, including all supported translations and English default. Update visible hints immediately when the user changes language.
- Keep at most one tooltip open. Dismiss its current presentation when focus moves to another field or the user taps outside; this transient closing does not mark it permanently dismissed. The X saves the individual dismissal as agreed.
- Tooltip positioning must remain readable on small screens, avoid covering input/save controls, and respect accessibility/font scaling.
- Validate independent dismissal, global hide/re-enable behavior, persistence, language switching, and compact layouts.



## 0.2.4 calculation decisions

- Effective salary periods are inclusive. A full month receives the configured amount once, even with no shifts or with absence statuses. Partial employment months and Week/Pay period views allocate by calendar days with eight-decimal half-up rounding. Salary history is retained when future pay changes. Archiving closes salary estimates at the archive date; restoring can open a new period.
- For monthly salary shifts, blank overtime/weekend/holiday rates add nothing. Explicit rates are additional payments; night percentages use the separate reference hourly rate. The monthly amount is never treated as an hourly or per-shift rate.
- Monthly bonus/deduction items belong to the final calendar day of their month for arbitrary reporting ranges. Monthly and yearly reports count them once. Attendance never creates a penalty automatically.
- English remains default. All new controls and field help have reviewed entries for mk/de/it/es/fr/sr/pt-BR/el.
- Cloud validation/report code must be deployed with this version before new absence statuses and salary/adjustment backups are tested in cloud restore.

## Optional Google Play update offer and AAB procedure

- Version 0.2.4 (code 14) includes a localized, branded Update / Later offer backed by Google Play flexible updates. Downloading happens in the background; installation is offered when the download is ready.
- Later postpones the same available version for 24 hours. A newer available version can be offered immediately. Local work remains available without accepting an update.
- For each AAB release, increment versionCode, run debug/release unit and lint checks, instrumented UI checks, and verify the signed bundle. Deploy compatible backend validation/report code before cloud restore testing.
- Upload the signed AAB to the existing closed-testing track and retain all three configured tester groups. Review release notes and the live integration gate before rollout.
- Test the real update flow with a Play-installed eligible older build and a higher version on the same tester track, using the same app identity/signing. Verify Update, Later, background download, ready-to-install/restart, and saved-data preservation. A sideloaded debug APK alone cannot prove Google Play eligibility.
- This build does not claim that its new AAB has been uploaded or that the physical Google Play update flow has been verified.

## Final 0.2.4 build validation

- Build source: 672f67321d218ec35a0d2184a4f941bb5ea137b8 on premium-analytics-upgrade.
- GitHub build run 37153289660: 69 debug unit tests, 69 release unit tests, debug/release lint, APK build, signed AAB build and bundle signature verification passed.
- Backend: 17 tests passed; translation generation verified 466 messages across eight translated languages.
- GitHub UI run 37153289651: 14 instrumented tests passed, including field hints, language switching, independent hint dismissal/global hide, job-specific day replacement and optional update offer.
- APK SHA-256: b152c8b158a19ed5dc124983ba6eb008ca08bf5f740bc2b129f5ada5220f3403.
- AAB SHA-256: 1e6f6dd971c62d5093c45ffd1b6a37e56f396a1d90350e559e2eef752d09a263.
- PTS-Firebase-v14.zip contains compatible source, private rules and a project-scoped deploy script; it does not seed jobs/shifts or contain private keys. Deploy with bash deploy.sh core, then verify new-field cloud backup/restore.
- No new Firebase deployment, Play upload/closed-test rollout or physical Google Play update flow is claimed by this validation. Automatic report emails still require a verified sender/secret and report-mode deployment.

## 0.2.4 closed-test submission — 2026-10-03

- User supplied a Firebase v14 Cloud Shell completion screenshot showing successful updates of all eight core functions. Live app sign-in, entitlement and new-field cloud backup/restore still need device verification; automatic report emails remain undeployed.
- Signed AAB versionCode 14 / versionName 0.2.4 uploaded to existing Closed testing – Alpha track 4699298164505386169, release 4.
- Release name: 0.2.4 · Field hints & monthly pay. English release notes cover field help, monthly salaries, adjustments, job-specific day statuses, reports/backups and optional Play update offer.
- All three existing tester lists remain selected: BokiTESTER, StayGuide Internal TEST, WarrantyCave Closed Test.
- Full rollout requested only within this closed-test track; no production rollout. Publishing overview shows Changes in review, with automated quick checks still running before Google review.
- Native debug symbols warning is nonblocking; no unsupported-device changes or validation errors were shown.
- Managed publishing was already off; approved changes can publish automatically. Do not claim availability to testers until Google approves/publishes this release.

## Latest availability and next-update QA — 2026-10-04

This section supersedes the older “in review / live deployment pending” release wording above.

- User screenshot 39532.jpg shows Closed testing release 0.2.4 · Field hints & monthly pay, bundle 14 (0.2.4), 100% Available on Play; rollout began October 3, 2026 at 23:28 as displayed in the screenshot. Production remains unpublished.
- User reported that the expected in-app update request did not appear. Keep this as an open device-QA item for the next release; do not label it fixed based only on source/UI tests.
- Source history confirms the Play update integration was first introduced by commit 68ae8a72690ff9a5017c4557e6599e393d3129f3 in 0.2.4. The published 0.2.3 (13) did not contain PlayUpdates or the app-update dependency, so it could not show the new PTS in-app offer for 0.2.4.
- For the next higher version, verify on a Play-installed 0.2.4 with an eligible tester account: offer on foreground/resume, Update/Later, 24-hour deferral, background download, restart/install and preservation of local jobs/shifts. Record the installed build, offered build, Play account/track eligibility and whether Later or Play auto-update explains a missing offer. Fix any reproducible failure found.
- User noticed another one or two situations needing correction. Their descriptions are not yet supplied; capture them individually with steps, expected behavior and screenshot before implementing. Do not invent these issues.
- Resend work is explicitly deferred by the owner until the website work in the other session is finished and the email/sender path is chosen. Do not purchase a Resend upgrade or change the website from this task.
- Current service evidence and the completed PTS-only Play grant are recorded in SERVICE-STATUS-2026-10-04.md. Purchase, RTDN and cloud/device QA remain separate from release availability.

## User-reported calendar marker correction — 2026-10-04

- Owner's latest decision (10:55 Europe/Skopje) supersedes the earlier proposal to hide the absent job's dot: keep the job marker for Vacation, Sick and Non-working day, visibly marked with a red minus sign. Use the same non-work distinction for Off; preserve the job identity/color so two jobs on the same date remain distinguishable.
- Scenario: mark the factory's day non-working while Wolt still has a Work shift on that date. The factory marker remains with a red minus; Wolt keeps its normal work marker, hours, pay and reminder. Scope changes only to the selected job and shift-start date.
- Confirmed current visual defect in CalendarScreen: all kinds derive ordinary job-colored dots from jobIds, so an absence currently looks like a Work shift. Next update must distinguish absence markers from ordinary Work markers, retain the status in day details and provide an accessible job/status description.
- Vacation, Sick, Non-working day and Off must not contribute worked hours, worked-shift counts, hourly wages, daily/per-shift pay or work extras. For hourly/daily jobs the record contributes zero earnings; replacing a paid Work shift removes its former earnings without creating an additional automatic deduction.
- Fixed monthly salary is unchanged by these day statuses. Only an explicit manual monthly deduction changes that salary estimate; do not prorate it automatically for absence.
- Normal shift reminders are not scheduled for these non-work records; another job's Work reminder on the same date remains independent. A global Holiday flag on an actual Work shift retains its normal pay/reminder rules.
- Verify factory + Wolt on one date for Vacation, Sick and Non-working day: factory marker remains with red minus, Wolt marker/pay/reminder remain, factory worked hours and hourly/daily pay are zero, fixed salary remains unchanged, and returning to Work restores the normal marker/calculation/reminder. Check overnight start-date scope, localized/accessibility status and saved/cloud preservation.
- Recorded requirements for the next update only; no Android patch, build or Play upload has been performed for this correction yet.

## Reminder behavior checked — 2026-10-04

- User asked whether holiday/vacation/non-working statuses can leave unnecessary shift reminders.
- Current Reminders.kt cancels alarms for the old schedule, then schedules only kind == Work entries. The receiver reloads local data and checks kind == Work again before notifying. Saving a job-specific non-working replacement therefore removes the original work reminder in the normal save flow.
- Preserve independent reminders for another job on that date (factory non-working, Wolt working).
- Global Holiday is a pay/calendar classification and does not automatically mean every job is off. A Work shift on a holiday still needs its reminder; a specific job/day changed to Non-working day, Off, Vacation or Sick must not notify.
- Device regression check alongside the calendar marker correction: schedule near-future reminders for two jobs, change only one job to a non-working status, confirm its prior alarm is canceled and only the other job notifies. Also verify returning that job to Work schedules its reminder again.
- No unnecessary reminder was reproduced on device in this session; this is verified source behavior plus pending physical QA, not a claim of a newly fixed reminder bug.

## User-reported rotation Days input — 2026-10-04

- User screenshot 39566.jpg and report: Backspace cannot clear the single default digit in a rotation block's Days field; entering a replacement first and then deleting the old digit is required.
- Confirmed source cause in ExtraScreens.kt / PatternDialog: Days renders block.days.toString(); onValueChange only accepts value.toIntOrNull(), so clearing the field is ignored and the previous number reappears.
- Next Android update: preserve editable text separately from the validated day count, allow a temporarily empty field and normal cursor/selection behavior, then validate a positive supported count on Preview/save. Empty or invalid text must show a clear field error, never silently generate the old/default count.
- Keep input state associated with the correct cycle row when adding/removing rows or changing jobs. Verify 2 -> empty -> 3, 1 -> empty -> 2, multi-digit Backspace, paste, row add/remove, job reset and existing valid rotation previews.
- Recorded for the next update; no Android patch, build or Play upload has been performed for this issue yet.

## Account/cloud status screenshot — 2026-10-04

- User screenshot 39568.jpg shows the signed-in account, Subscribe yearly at $2.35/year, enabled Restore purchases, and disabled cloud backup/restore buttons. The cloud section simultaneously says "Automatic backup enabled on this phone", "Last cloud backup: —" and "Premium is required".
- Important owner clarification: this screenshot is from a Premium account; the owner confirms "Ja sum premium". Do not classify the owner as Free or resolve this report only by changing the backup message. Treat it as a reported failure to recognize/display Premium and enable its cloud actions, pending live entitlement diagnosis.
- Confirmed UI inconsistency in AccountScreen.kt: the enabled message depends only on repo.bound(uid), a local device/account binding, while the buttons depend on status.premium. The screenshot proves that this screen is treating Premium as unavailable, not that the user lacks Premium. A binding alone also does not prove a successful upload.
- PtsApp.kt initializes/resets account state with premium=false before the server response. AccountDialog does not receive accountResolved; if refreshAccount's server call fails, its exception is swallowed and the account screen can continue showing the unresolved/default non-Premium state. This is a concrete diagnostic path, not proof of the cause on the user's phone.
- Next update diagnosis must distinguish loading/connection/verification errors from a successfully resolved Free account; show loading or a retryable error instead of presenting an unresolved status as a confirmed lack of Premium. For an eligible, server-verified Premium account, enable backup/restore and show Premium consistently across Account, analytics and ads.
- Verify getAccountStatus active/expiry and test-access state for the correct Firebase UID, restored Play purchase/verification where applicable, refresh behavior and backend errors. Do not assume paid subscription versus owner/test access, purchase failure, expiry or a network error without evidence. Do not ask the owner to repurchase to work around this report.
- Make the cloud status agree with resolved entitlement and actual upload evidence. Keep the last successful backup timestamp distinct from configuration; PremiumRepository can bind a device before an upload succeeds.
- Preserve the account binding and local jobs/shifts. Device QA must include the owner's Premium account, unresolved/failed refresh, valid Premium backup/restore, expired/Free status with a retained binding, failed upload and account switching.
- PlayBilling.kt obtains the displayed annual price from the annual base plan's Google Play formattedPrice. The screenshot alone does not establish why it differs from the US base price.
- This is source diagnosis and next-update planning only. No Premium recognition fix, successful payment verification or physical new-field cloud backup/restore is claimed.

## Settings button sizing and additional Premium evidence — 2026-10-04

- Owner screenshot 39573.jpg requests smaller buttons in Settings. Scope includes Account & Premium, Privacy choices, Website, Privacy policy, Save backup file and Restore backup file; align the nearby Save actions with the compact style.
- Next Android update: reduce visible button height and excessive vertical padding, use consistent compact icon/text proportions, and reduce unnecessary section spacing where it contributes to the oversized appearance. Preserve readable localized labels and comfortable accessible touch targets; check small screens and larger font settings. Do not remove actions or change their behavior.
- This screenshot also explicitly shows "Premium active" and "0.2.4 · PTS Premium" in Settings. Combined with the earlier Account screenshot's disabled cloud buttons / Premium-required message, this supports the owner's reported inconsistent Premium presentation across observed screens. The captures are at different times; do not claim simultaneous state or a confirmed backend cause.
- Reproduce the Settings -> Account transition and account refresh/resume flow on the owner's Premium account, recording whether the status changes or cloud controls disagree. Resolve the entitlement/status issue separately from the button sizing.
- Recorded for the next update; no Android layout patch, build or Play upload is claimed.

## Website links, support email and release timing — 2026-10-04

- Owner requests the app's Website and Privacy policy actions use the new paytimeshift.com website, and support email be visible in the app as bokimk.ap@gmail.com.
- Current PtsApp.kt still links to https://pts-developer.bokimkd.chatgpt.site and its privacy.html. These are unchanged in Android source in this session.
- Verified the owner's PTS-Website-9-Languages-2026-10-04.zip: its homepage links to privacy-premium.html; this policy applies to 0.2.0 and later. The separate privacy.html is the historical 0.1.7 local-only policy and must not be the new app's primary policy link.
- Next implementation targets: Website https://paytimeshift.com/; Privacy policy https://paytimeshift.com/privacy-premium.html; visible support address bokimk.ap@gmail.com with a mailto action that opens the user's email app without sending automatically. Keep labels localized and consistent with the planned compact Settings buttons.
- The website archive also contains delete-account.html. Verify public availability of the current policy and account-deletion page at release review; changing Play listing metadata is a separate reviewable action.
- Public checks in this session could not verify the domain: web retrieval was inaccessible and direct HTTP checks returned 502. This does not establish the cause or publication state; recheck once website publication completes.
- Owner's latest instruction at 11:00 Europe/Skopje: wait for website publication, then make the AAB. Do not start a new AAB/release upload now. Confirm homepage and current privacy page load publicly before applying the links, completing the agreed fixes/checks and building the next incremented version.
- Only this plan was updated for these requests; no Android source changes, website deployment or new AAB were performed.

## Calendar short-rest indicator — 2026-10-04

- Owner screenshot 39575.jpg shows the selected day's existing "0h between shifts" warning. Owner requests a small exclamation mark on the calendar day whenever the rest interval is below the configured minimum, without disturbing job dots.
- Next Android update: show one compact warning-colored "!" in a separate corner of the relevant date cell. Keep the date number, normal job dots, red-minus absence markers, holiday background and selected-day outline readable and in their existing roles.
- Use the same short-rest decision as the detailed day warning, including only actual Work shifts and preserving cross-job and overnight behavior. Recompute after schedule edits and minimum-rest setting changes; below the minimum warns, equal/above does not.
- Determine intervals with neighboring dates/months so an overnight shift or a prior-month shift cannot hide a short-rest warning at the month boundary. The cell indicator and selected-day warning must agree for the associated date.
- Keep the existing detail below the shift cards when the user selects the day; add a localized accessible short-rest description to the date cell. The icon is a warning only and does not change shift dots, earnings or reminders.
- Verify two jobs on one date, overnight and month-boundary intervals, threshold equality, disappearance after correcting the schedule, light/dark themes, small screens and the new absence marker coexistence.
- This is an addition to the next-update plan; no Android patch, AAB or publication is claimed. The owner's wait-for-website instruction remains in effect.


## Current implementation — 0.2.5 / 15, 2026-10-04

The owner confirms the website is available and authorizes a new AAB, superseding the earlier wait instruction. Android source eaf038ab23751d11e6a37a2aa3968e4a3a26e7b8 implements the agreed red-minus absence markers (with contrast on red job colors), the separate short-rest exclamation mark, editable rotation counts, resolved/error-aware Premium cloud controls and refresh/purchase coalescing, compact Settings buttons, paytimeshift.com/current policy links and visible support email. Existing non-working zero hourly/daily pay, unchanged fixed monthly salary and Work-only reminder filtering are preserved. Historical planning/diagnosis notes above describe the state at their recording time; use RELEASE-0.2.5.md for the current build result.

Final build 37192622764 and UI validation 37192622779 passed: 73 debug + 73 release unit tests, 17 UI tests and 17 backend tests. PTS-0.2.5-15.aab is signed with the existing upload certificate; bundle/manifest validation passed. SHA-256 49b29ce9d68ee674c718d9130c0d14c1862a9ae96e1ab57b46a62774daa000b3. The artifact was saved for delivery. The build is not a Play upload or rollout. Owner-account cloud backup/restore, license purchase/restore and the 0.2.4 -> 0.2.5 update offer still need physical-device verification. RTDN, deferred Resend/report mail and Meta integration remain open.


## Account buttons and physical update report — 2026-10-04

- Owner screenshot 1791111764959.jpeg identifies Restore purchases and Sign out still being taller than Refresh account and the cloud controls. Prepared fix commit 322d2733040b49e99c4bd85a99539709f19f2ab3 on fix/account-compact-buttons, draft PR #2: https://github.com/bokimmkd/paytimeshift-android/pull/2.
- Exactly these two existing controls now use the matching 36 dp minimum visible height and horizontal 8 / vertical 3 dp padding. Material touch targets and handlers are preserved. Diff check and connector source readback passed; no new AAB or Play rollout for this patch. Apply this patch before the next incremented Android release (0.2.6 / versionCode 16).
- Live Alpha page now confirms 0.2.5 is Available to selected testers. Owner reports the update window appeared, but has not yet confirmed the installed version. Do not classify download/installation as failed or successful from the window alone.
- Current PlayUpdates uses FLEXIBLE: consent starts background download, DOWNLOADED shows Restart to update, and completeUpdate performs installation/restart. Ask for the version shown in Settings (0.2.4 versus 0.2.5) and whether the ready/restart step appeared. The UI has no download progress indicator. Launch/result failures and the download() call to later() warrant inspection if the phone remains on 0.2.4; no failure cause has been reproduced on this device.


## Release candidate — 0.2.6 / 16, 2026-10-04

Owner confirms that fixing the two remaining account buttons completes the release candidate. PR #2 is now included in af338cdf07f58bbb08225ac8595d8a84a73d8a28, with version increment to 0.2.6 / 16. Build 37198005522 and UI validation 37198005511 passed (73 debug + 73 release unit tests, 17 UI, 17 backend; no failures). Signed PTS-0.2.6-16.aab is validated, has the existing upload certificate, and is saved for delivery. SHA-256 7908710c56beac49aaa2d8e470100ccf387aa750aae063a7262e6933fc5bbedb. See RELEASE-0.2.6.md for artifact evidence. No 0.2.6 Play upload or rollout; currently published Alpha remains 0.2.5. The owner's installed-version/update-restart confirmation and physical payment/cloud checks remain open.


## Alpha submission and device update confirmation — 2026-10-04

Owner confirms Settings shows 0.2.5; the earlier physical installed-version question is resolved. Owner explicitly authorizes publication of 0.2.6. The exact validated AAB16 is uploaded and submitted on existing Alpha at 100%, release 6 / "0.2.6 · Compact account buttons". Publishing overview shows Changes in review with quick checks running and managed publishing off. Availability to testers is still pending; see RELEASE-0.2.6.md and saved PTS-0.2.6-Play-review-1791113363940.jpg proof. No production or backend deployment; physical payment/cloud tests and RTDN/report-email/Meta work are unchanged.


## 0.2.5 -> 0.2.6 offer report — 2026-10-04 14:10 Europe/Skopje

Owner reports 0.2.6 approved but no in-app update offer. A fresh Alpha page now positively shows "Latest release: 0.2.6 · Compact account buttons" and "Available to selected testers"; publication is confirmed.

Reviewed the exact candidate source af338cdf07f58bbb08225ac8595d8a84a73d8a28: MainActivity creates PlayUpdates and passes it to PtsApp; PlayUpdates checks on lifecycle onResume. The app does not continuously poll while remaining in the foreground. The source is unchanged from 0.2.5, whose installation the owner already confirmed. shouldOfferUpdate allows newer versionCode 16 even when versionCode 15 was deferred; a 15 deferral is not a confirmed explanation for the missing 16 offer. PtsApp waits until loaded/not saving and account/edit/pattern/analytics dialogs are closed before showing the prompt.

Next phone diagnostic: close/reopen PTS on the main screen; inspect whether the same Play account's PTS listing offers Update, without installing first if testing the in-app prompt. Console availability does not reveal the device's appUpdateInfo response. Do not assert a propagation delay, a reproduced client failure, or successful 0.2.6 installation yet. No new source patch/AAB/rollout for this report.


## Update retry and turquoise Add job — 0.2.7 / 17, 2026-10-04

Owner reports the Play listing offers Update but the PTS offer does not reappear after returning to the main screen; screenshot 1791116039853.jpeg. Exact device Play SDK response is unavailable, so the cause remains unproven. Owner requests a new AAB and a turquoise Add job action.

Source 093648439db27caf0cb0c529f916de52389f5ac6 adds foreground retries, request/session coalescing and timeout handling, available-update Store fallback, explicit-only per-version postponement and protection of downloaded/Ready events. Add job is now turquoise with white icon/text. Build 37202260071 and UI workflow 37202260083 passed: 82 debug + 82 release unit tests, 18 UI, 17 backend; no failures. Signed PTS-0.2.7-17.aab validated and saved for delivery; SHA-256 4725317aa5766d2889470b9c6cd8d449868668ea07a2a3aa82bd8a9132cb221c. See RELEASE-0.2.7.md for artifact and certificate evidence.

0.2.7 is a prepared candidate, not a Play upload/publication; currently published Alpha remains 0.2.6. The old installed client needs 0.2.7 installed before the improved checking logic applies; real update-offer/download/restart verification requires 0.2.7 -> a higher Play version. Physical payment/cloud checks and RTDN/report mail/Meta work remain open.


## 0.2.7 Alpha submitted — 2026-10-04 14:49 Europe/Skopje

Owner authorizes upload/publication. Exact validated AAB17 uploaded to existing Closed testing - Alpha, release 7, 100% rollout, name "0.2.7 · Update checks & turquoise Add job". Publishing overview confirms Changes in review; quick checks are running and managed publishing is off. Availability to selected testers is pending. See RELEASE-0.2.7.md and saved PTS-0.2.7-Play-review-1791118183371.jpg proof. Earlier candidate-only status is superseded. No production or backend deployment; physical update/payment/cloud checks and RTDN/report mail/Meta remain open.


## Cloud upload blocked but mislabeled as connection failure — 2026-10-04 15:48 Europe/Skopje

Owner screenshots 39613.jpg and 39615.jpg show the cloud upload confirmation followed by "Could not connect. Check your connection and try again." Cloud actions are enabled; the screen shows "Choose cloud restore or enable backup on this phone first" and an existing backup timestamp 2026-10-04T11:03:58.351. This is not evidence that the owner's phone has lost internet or lacks Premium.

Source diagnosis at b3b2b709cc45706be72094e259bdc48fa664dea6: AccountScreen.kt's confirmation checks status.revision==0 before binding an unbound phone. With an existing nonzero cloud revision it throws IllegalStateException("Restore existing backup first.") before calling reportPreferences or backup. PremiumRepository.kt's cloudError does not map that exception; it falls through to the generic connection message. This matches the visible existing-backup/unbound state closely. The actual device exception and network request were not captured; do not claim a backend outage or a proven phone call trace.

Next Android update (expected 0.2.8 / 18): present this existing-backup condition accurately before upload confirmation, using a localized explanation and appropriate restore guidance. Preserve the guard against overwriting an existing cloud backup from an unbound phone. Keep successful binding/upload status distinct and make mapped local validation errors specific rather than connection failures. If a cloud restore would replace newer local jobs/shifts, explain that clearly and allow saving a local backup first; do not restore, delete or overwrite automatically. Add a meaningful regression for the existing nonzero revision/unbound case and error presentation. Retain existing cross-phone revision conflict and empty-data protection.

This turn diagnoses and records the issue only. No new Android patch/build/Play release, backend deployment, successful backup/restore or device data mutation is claimed. 0.2.7 was previously submitted to Alpha; its current approval/availability was not rechecked in this turn.


## Clear warnings in shift preview — owner agreed, 2026-10-04 15:58 Europe/Skopje

Owner screenshot 39617.jpg shows Shift patterns review (17 / 17 selected) with ambiguous start-time-only warnings. Reviewed Models.kt: gapMinutes is correctly calculated from first.finishes to next.begins, but ScheduleWarning.message displays first.date/start and next.date/start. ExtraScreens.kt compares the selected preview rows with retained existing shifts across jobs and displays only the first three warning strings. Thus a warning saying 07:00 and 15:00 can correctly mean zero rest when the 07:00 shift finishes at 15:00; the wording hides this distinction. No incorrect phone arithmetic is established by the screenshot.

Owner asks to add the clearer explanation to Shifts. Agreed next Android update, 0.2.8 / 18: in the shift pattern/import review, display the job name and full start-end interval for each compared shift (including end date or +1 for overnight), then a clear localized rest-gap or overlap explanation. For example, "Job 1 · 07:00–15:00 → Wolt · 15:00–18:00; 0h rest between shifts" (illustrative values, not a claim about the owner's stored schedule). Identify overlaps clearly and keep long/localized labels readable. Make existing/new shifts distinguishable when useful. Preserve the shared warning calculation, job minimum-rest settings, Work-only filtering, selection behavior and explicit Add/Import save; warnings remain advisory. Use the same clear shift identity/time presentation wherever warning detail is shown, keeping the calendar cell's small indicator compact.

This is recorded scope only; no implementation/build/Play publication in this turn. Keep this alongside the pending cloud guard/error-message fix and physical 0.2.7 -> next-version update validation.


## Delete shifts by date range — owner request, 2026-10-04 16:01 Europe/Skopje

Owner asks how to delete all shifts in a From/Until period while resolving an overlapping/new schedule. Current source has individual Delete shift and PatternDialog's "Replace existing shifts in this period" for one selected job; replacement removes all that job's entries in the range and requires a nonempty reviewed new schedule. There is no standalone date-range delete flow in the current reviewed UI. Do not describe replacement as delete-only or instruct the owner to deselect every preview row to achieve deletion (save is disabled and domain requires added rows).

Add a separate "Delete shifts in a period" action to the agreed next update 0.2.8 / 18, reachable from shift/calendar management. Select From and Until inclusively by shift start date, and select a specific job or explicitly All jobs; default to the selected job so another job such as Wolt is preserved. Before confirming, show the selected job scope, date range and number of entries affected, making clear whether non-working/day-status entries are included. Allow cancel and do not alter data before confirmation. Keep entries outside the selected scope and job definitions/settings. Recompute affected earnings, warnings and reminders after the confirmed local save; fixed monthly salary remains governed by its salary records. Use localized readable labels and meaningful range/scope/cancel tests. Preserve existing replace-with-new-schedule workflow for owners who want to swap rotations in one action.

Recorded requirement only: no deletion of the owner's data, Android implementation, new AAB or Play publication in this turn.


## Earnings Pay period diagnosis and date visibility — 2026-10-04

Owner screenshot 39619(2).jpg shows Pay period selected, No shifts added / 0h, and monthly next payments on October 15. Reviewed the current EarningsScreen, nextPayday, payPeriod and analytics paths: monthly payPeriod covers the previous calendar month of the next payday. For today October 4 and next payday October 15, both jobs' displayed periods are September 1–30; saved October shifts belong in October Month view. The screenshot does not establish lost shifts or incorrect arithmetic, nor does it provide a device database to verify the owner's exact rows. Existing periodsAndLeapMonth coverage explicitly validates this previous-month monthly rule. Do not silently change payment coverage or salary allocation.

Implemented a display-only clarification for the next Android update (expected 0.2.8 / 18): replace the ambiguous Current pay period heading with the existing localized Payday and covered dates label and show each job's full covered From–To dates even when there are no shifts in that range. Dates use the selected language locale; user job names remain literal. Preserve Month/Week calculations, currencies, payday settings and saved data. Reused existing translations in all nine supported languages; no new backend or data schema is needed. Source/diff/localization checks completed; Android compile/UI/device checks remain pending for this change. No new version increment, AAB, Play upload or backend deployment from this diagnostic.


## 0.2.8 / 18 implementation and publication authorized — 2026-10-04

Owner explicitly requests completing and uploading 0.2.8. Source now includes the four agreed changes: accurate existing-cloud-backup guidance with local-save option before confirmed restore; full job names/start-end/date warning details in pattern/import and calendar; standalone inclusive start-date deletion by selected job or explicitly all jobs, reviewed count/status scope and confirmation with stale-selection protection; per-job covered payment dates on empty/nonempty Earnings Pay period views. Existing pay calculations, cloud overwrite/revision guards and update-check logic from 0.2.7 are retained. New deletion saves use the normal local save/reminder/widget/cloud scheduling path. All new labels are localized in the nine supported languages.

Build/unit/lint/UI/signature validation is pending. No Play upload is claimed by this source record. Real update-trigger test requires a Play-installed 0.2.7 to see the published higher version 0.2.8, then verify offer, Later, download/restart and preserved data. Do not auto-update the test phone before this check.


## Restore must preserve this phone's appearance — included in 0.2.8

Owner reports two cloud restores changed Light to Dark. Current restore replaces AppData including backed-up appearance, explaining this source path; no device trace is claimed. Both confirmed cloud restore and confirmed manual-file restore now use withRestoredBackup: restore the saved work data/preferences while keeping the appearance currently selected on this phone (Light, Dark or System). Repeated restores must keep that selection, and the resulting local save must retain it across restarts. Tests cover all appearance modes, repeated restores, opposite-theme backup, restored work/preferences and serialized persistence. Source added before publishing 0.2.8; final build/UI validation is pending.


## 0.2.8 / 18 validated and submitted to Alpha — 2026-10-04

All five agreed fixes, including theme preservation for cloud/manual restore, are in source 1032424. Android/backend build 37210828765 and UI run 37210780258 passed: 92 debug + 92 release unit tests, 22 UI tests, 17 backend tests, lint and signed bundle verification. Exact signed AAB uploaded as versionCode 18 / 0.2.8 and submitted as the sole Alpha change. Publishing overview confirms Changes in review; tester availability remains pending. See RELEASE-0.2.8.md for evidence, artifact hashes and remaining physical update/affected-account restore checks. Owner clarifies backup/restore work for other tester accounts; the reported issue concerns the owner's permanent-grant account. No live account mutation, entitlement change, backend deployment or successful owner-phone integration check is claimed. Earlier pending validation statements for this source are superseded by this record.


## Physical update flow report and next update polish — 2026-10-04 18:03 Europe/Skopje

Owner screenshots 39641/39642/39643 show PTS New version available, Google Play flexible-update consent (8.4 MB displayed), and PTS Update ready. Owner reports Ready appeared almost immediately after Update, and Restart did not look like a conventional close/relaunch, but a newer version appeared afterwards. This is owner-reported installation success; exact before/after version numbers, download duration/cache state and retained shifts were not independently verified. Do not claim the one-second download or a premature Ready bug from still images.

Source PlayUpdates exposes Ready only for Play InstallStatus.DOWNLOADED, from its listener or appUpdateInfo, and Restart calls completeUpdate. It currently gives no PTS progress UI for PENDING/DOWNLOADING and clears the prompt before installation. Official Google documentation describes background download after consent and platform-managed installation/restart via completeUpdate: https://developer.android.com/guide/playcore/in-app-updates/kotlin-java. A quick or cached download is a possible explanation, not established device evidence.

Owner says the transition did not feel smooth. Next Android update scope: show a compact localized Waiting/Downloading indicator and byte-based progress when Play supplies a positive total; retain normal app use during download. Offer Restart only after confirmed DOWNLOADED, with no artificial delay for fast/cached downloads. After explicit Restart, show Installing until Play takes over; prevent repeated taps and recover clearly on completeUpdate failure. Resume should reconstruct Play state without reviving stale offers or downloaded events for an already installed version. Keep Later behavior, consent, optional updates, Store fallback and local data preservation. Add meaningful state-transition tests for slow/instant download, resume, repeated Restart and completion failure, and validate the next higher-version update on a real Play-installed device. Recorded scope only: no new application patch, version increment, AAB or Play submission in this diagnostic turn.


## Calendar actions menu clarity — owner agreed, 2026-10-04 18:35 Europe/Skopje

Owner screenshot 39649.jpg shows the Calendar overflow menu with Mark as holiday, Import roster, Delete shifts in a period and Share schedule. Owner agrees to include the following presentation changes in the next Android update:

- Keep these actions in Calendar. Order: Import roster, Share schedule, Mark as holiday / Unmark holiday for the selected date, a divider, then Delete shifts in a period.
- Show the selected date visibly beside the holiday action (for example, Mark as holiday · Oct 4), using the selected app language and locale. Update the date and mark/unmark wording when selection changes so the scope is clear.
- Give every action a consistent leading icon and aligned text. Separate the destructive date-range deletion with a divider and use the theme's accessible error/red color for both its icon and text in Light and Dark.
- Preserve action behavior: import/share keep their existing scope; the holiday flag applies to the selected calendar date across jobs and uses each job's configured holiday pay rules; deletion keeps its job/range review, affected count and explicit confirmation. Rearranging the menu must not modify saved shifts, rates or holidays.
- Verify the menu on small screens and with longer translations/font scaling, selected-date changes, already-marked holidays, Light/Dark, and deletion cancellation.

Recorded next-update scope only. No application implementation, version increment, new AAB or Play submission is claimed for this plan change.
