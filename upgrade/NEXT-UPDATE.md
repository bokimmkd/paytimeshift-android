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
