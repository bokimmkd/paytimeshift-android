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

- Scenario: mark a specific job's shift/day as Non-working day while another job (for example Wolt) still has a Work shift on the same date.
- Existing job/date replacement scope is correct: only entries for the chosen job/date are replaced. Preserve the other job's shift, reminder and pay.
- Confirmed source defect in CalendarScreen: dayRows currently includes every kind and derives normal job-colored shift dots from all jobIds, so a Non-working day, Off, Vacation or Sick record still looks like a work shift.
- Next Android update: normal colored shift dots must represent Work entries only. After marking the factory non-working, its work dot disappears while Wolt's work dot remains. Keep the non-working status visible in the selected-day details without counting it as a worked shift. Align the calendar's shift counts and accessibility description with actual Work entries.
- Existing arithmetic is already correct for a specific shift converted to a non-working status: hourly/per-shift jobs produce zero paid hours, base pay and extras for that record. If it replaced a planned paid shift, the month's estimate falls by that shift's previous earnings; no additional automatic deduction is created. Other jobs' earnings are unaffected.
- Fixed monthly salary remains unchanged by attendance; reductions only come from explicit monthly deductions. Preserve these rules and Android/server agreement.
- Verify one date with factory + Wolt: mark factory non-working, confirm only factory replacement, Wolt marker/pay remain, factory hourly/daily earnings become zero, fixed salary remains unchanged, then change back to Work. Check overnight start-date scope and saved/cloud status preservation.
- This correction is recorded for the next update; no Android patch, build or Play upload has been performed for it yet.

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
