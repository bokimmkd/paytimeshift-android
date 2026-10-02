# PTS · Pay Time Shift — 0.1.5

Android app: `com.paytimeshift.pts`, versionCode 6, Android 8.0+.

## Play closed-test candidate 0.1.5

- Signed, non-debuggable release AAB with a dedicated private upload key.
- Matching week/month cards with equal height and a single-line hours/planned row.
- Validated final AAB with bundletool 1.18.3 and jarsigner; target SDK 36 and 16KB alignment for bundled 64-bit native libraries. 32 release unit tests passed; lint: zero errors, six advisories and one hint. No Play Console upload/review or Android 16 native test is claimed.
- compile/target SDK 36, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- A new install starts with no jobs, shifts or holidays. No fixture or local database is bundled. Example data is available only after an explicit user choice and confirmation. Automatic Android backup/restore is disabled. Existing local data is never cleared by application startup.
- Compact design and functionality retained from 0.1.4. Main and secondary screen headings now have icons and short accent-colored Macedonian/English descriptions.
- GitHub workflow builds/test-checks debug on pushes. Manual workflow_dispatch also builds the signed release AAB using repository secrets. Missing secrets cause an explicit failure.
- Private upload key/passwords are never committed to GitHub. The old bundled prototype debug key is used only for debug builds.
- Google Play's installed APK signature differs from the prototype debug APK. Before switching an existing test phone to Play, export a manual JSON backup; the debug APK may need uninstalling. New testers start empty.
- Premium billing, live ads and automatic cloud backup remain unconnected. This AAB tests local features and design, not purchases.
- Repository: https://github.com/bokimmkd/paytimeshift-android. GitHub Actions builds APKs on source changes; manual AAB builds require the four repository signing secrets. See `PLAY-CLOSED-TEST.md`.

## Changes in 0.1.4

- Compact shift editor: job dropdown; date/day type, start/end, and break/bonus in two columns; smaller selection controls; compact paid-break/current-price checkboxes; one-row earnings preview. Optional notes remain multiline and Delete keeps its confirmation.
- Compact job editor: paired hourly prices, frequency/payday, shift times and separate rest/reminder options. No payroll or data changes.
- Two-column review of generated/imported shifts, with independent selection and a lazy scrolling list.
- Compact settings: paired language/appearance, currency/time display, shorter reminder/rest controls, and side-by-side manual backup actions.
- Unified smaller save/cancel buttons with icons; section headers and date/time controls also have icons.
- Paired forms fall back to one column below 240dp available width or above 1.3 font scale. Labels and notes can wrap; all forms remain scrollable.
- The four main tabs are unchanged from 0.1.3, as requested.

## Features introduced in 0.1.3

- Per-job concrete prices for regular, overtime, Saturday, Sunday and public-holiday hours. Amounts accept a decimal point or comma in the editor.
- Overtime begins after the configured number of **paid hours per shift**. This is a per-shift threshold, not a weekly payroll rule.
- Daily earnings are calculated from paid hours and the applicable hourly price. Fixed per-shift pay remains an optional separate mode.
- Payment frequency is a dropdown: monthly, weekly, every two weeks, or custom payday.
- Select a calendar date and use **⋮ → Mark as holiday**. Holiday dates are stored locally and included in manual backups. The mark applies to all jobs; each job uses its own holiday price. No country-specific holiday calendar is loaded automatically.
- A holiday price replaces the weekend price for that date. When overtime overlaps with a weekend/holiday, the higher of the day price and overtime price is used, rather than adding two full hourly prices. A blank price inherits the regular price; a blank overtime price inherits the day price. Zero is a valid price.
- Shifts that cross midnight split their hourly prices at the calendar-day boundary.
- When editing a job, the visible switch **Apply prices to today's and future shifts** is on by default. It updates their prices and currency while retaining each shift's entered times, breaks, notes and bonus. Disable it to retain all existing snapshots. Past shifts always retain their previous prices.
- An existing shift also offers **Use current job prices** for an explicit refresh of its saved prices.
- Shift editor displays a live estimated daily amount and paid hours. Calendar's Add shift uses the selected date.
- All four tabs retain the compact navy/teal reference style. The job editor loads sections lazily. Jobs, shifts, patterns and import share a branded scrolling editor with fixed bottom actions and visible validation messages; settings use grouped cards. Macedonian is the default, with English available in Settings.

## Calculation details

An untimed unpaid break is allocated proportionally across the shift, including date and overtime boundaries. A paid break does not reduce paid time. Night additions remain a configurable percentage of the regular hourly price. Explicit bonuses are added once. Fixed-pay shifts use their fixed amount plus bonus; hourly prices and percentage additions do not apply to fixed pay. Off/vacation/sick-day entries have zero estimated pay.

Legacy shifts retain their saved percentage rules until explicitly refreshed through the job/shift controls. New shifts convert old job percentage rules into concrete overtime/Sunday prices. The data schema remains version 1 with optional new fields; older backups remain readable.

Amounts are estimates. No taxation, payroll deductions or currency conversion are performed. Duration uses local wall-clock time, and a whole shift is assigned to its start date when selecting a report; DST and allocating earnings across reporting periods remain limitations.

## Existing local features

Multiple jobs; separate shift/break/reminder/rest settings; decimal rest-gap input; overlap warnings; weekday repeating patterns with review and duplicate prevention; local reminders; home-screen widget; Latin-script image/PDF OCR with editable reviewed import; monthly PDF/PNG schedule sharing; manual JSON backup and confirmed restore; archive/restore jobs.

Notifications, widget launcher behavior, live image/PDF OCR and Android sharing still need physical-phone checks. OCR is not guaranteed for Cyrillic or complex roster tables.

## External services

Live AdMob/consent, Google Play Premium payments and purchase verification, and automatic cloud backup/restore remain unconnected. The Premium card does not initiate a real payment. Manual local backup works independently.

## Build and validation

0.1.4: 32 unit tests passed and Android lint reported zero errors and two advisory warnings. The debug certificate is unchanged. Native installation succeeded and the compact shift form displayed all controls in one viewport at 393dp width. The software emulator reported system/startup and app focus-event ANRs when opening the keyboard; native input/performance is not counted as a complete pass. After waiting for the emulator to recover, entering and saving a bonus of 5 succeeded; all 3 jobs and 34 shifts remained. The job editor displayed paired hourly prices and Settings displayed its compact paired controls. The final APK was installed successfully over the candidate build. Physical-phone performance testing remains necessary. Checks are recorded under `validation/0.1.4`. Earlier 0.1.3 native upgrade and calculation evidence follows below.

For debug validation use JDK 17 and Android SDK 36:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

32 unit tests passed, including hourly overtime/weekend/holiday prices, priority rules, midnight splits, fractional thresholds, paid/unpaid breaks, legacy calculations and backup round trips. Android lint: zero errors, two advisory warnings (backup declaration and widget overdraw). Debug APK uses the same signing certificate as 0.1.2.

Android upgrade check: 0.1.2 installed, seeded with 3 jobs and 34 shifts, then upgraded to 0.1.3. Stored data remained byte-equivalent immediately after upgrade. The software emulator displayed Android system/System UI ANR dialogs at startup and app focus-event timeouts when opening the job form. Traces showed composition/layout and the normal recomposer, with no fatal runtime exception or blocked application lock. The final build uses lazy job sections to reduce initial composition work; these emulator delays are not counted as a complete native performance pass. Native job editor test changed the regular price from €6 to €10 and saved successfully. Today/future shift snapshots became €10 while the October 1 history stayed at €6; all 3 jobs/34 shifts remained. The native payment dropdown displayed all four options; selecting Weekly and saving updated the stored job cycle successfully. Current checks are under `validation/0.1.3`; earlier validation is under `validation/0.1.2`.

The bundled debug keystore preserves prototype upgrade compatibility only. Use a separate release/upload key for production.
