# PTS — next update requirements

Recorded: 2026-10-03. These are implementation requirements, not a claim that all features below are released.

## 1. Change the status of a scheduled day (pending)

- In Add Shift / Edit Shift, support Work, Off, Vacation, Sick, and Non-working day.
- A non-working status must be selectable even when that job already has shifts on the date. Do not reject it with "This shift already exists".
- Before replacing existing entries, list the affected entries and ask for explicit confirmation. Cancel must leave data untouched.
- Apply the change only to the selected job and selected shift-start date. If two jobs share the date, either job can be changed independently; never change the other job.
- A full-day non-working status replaces all shifts for the selected job/date. Editing one shift into that status must also review any additional shifts for that same job/date.
- Changing a non-working status back to Work must explicitly replace the status entry instead of leaving contradictory work and absence records.
- Hide time, break and bonus inputs for non-working statuses. Existing Off/Vacation/Sick behavior remains zero estimated pay for hourly/daily jobs; paid leave rules are a separate future decision.
- Preserve local storage, manual/cloud backup, colleague schedule export/import, translations, reminders and analytics compatibility.
- "Non-working day" is a job-specific calendar status; it is distinct from the global Holiday flag used for holiday pay on worked shifts.

## 2. Fixed monthly salary per job (pending)

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

## 4. Monthly deductions and bonuses per job (pending)

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
