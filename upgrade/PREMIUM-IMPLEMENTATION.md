# PTS Premium upgrade — accepted specification

Default language: English. Preserve existing selected language. Supported: English, Macedonian, German, Italian, Spanish, French, Serbian, Portuguese (Brazil), Greek.

Annual auto-renewing subscription: pts_premium, base plan annual, USD 1.99/year (localized Play pricing). Buy us a coffee ☕. Benefits: no ads, automatic cloud backup, restore to a new phone, advanced work analytics, monthly and yearly reports.

Implement in this upgrade: optional per-job costs with seven standard categories and custom names; amounts in job currency; per shift, workday, ISO workweek, or working month; enabled switch; local/manual/cloud persistence and old data migration. Premium entry in Earnings, gross minus work costs, gross and real hourly values, category percentages, by-job analysis, overlapping time classifications, monthly comparisons, twelve-month yearly trends and highlights. Premium PDF view/share/print/email, branded PTS. Automatic verified-email monthly/yearly reports default ON; editable account settings. Firebase accounts, server-verified Play subscription entitlement, Premium-enforced backups, restore/conflict protection, account deletion. Free AdMob with consent; paid removes ads.

Gross engine must stay unchanged. Reports are estimated real earnings after work-related costs, never official net salary. No tax/government/payroll deductions. Never aggregate different currencies. Existing jobs/shifts retained; fresh install empty.

Cost convention: per workday is a distinct shift start date. Weekly costs are once per ISO week containing work and allocated to its earliest workday, preventing double counting across months/years. Monthly costs are once per month containing work. Current optional cost rules estimate historical costs. Overnight shifts belong to their start month as in the existing gross reporting engine. Worked hours exclude all breaks, while paid hours include paid breaks. Night/weekend/holiday paid hours overlap regular/overtime and must not be added together.

External readiness must be verified before calling this release functional: Firebase project and Android config, Auth, Firestore/Storage deny-by-default rules, Functions deployment, subscription product annual base plan, Play Developer API access and RTDN, verified email sender/secrets, AdMob linking, billing license tests, migration and currency/report regression tests. Firebase Blaze final billing confirmation belongs to the user; no fabricated credentials, purchases, accounts or reports. No other app or production release may be changed.
