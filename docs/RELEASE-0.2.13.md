# PTS 0.2.13 (23) — Widget design and distinct job colors

Based on premium-analytics-upgrade, 6cd213dfaca3b216145f7ea2353e09cc0505c38a / application 0.2.12 (22).

The widget now follows the approved visual: a white/dark rounded surface, clock logo, separate muted dates, thin dividers, a colored stripe and briefcase circle for each job, and a matching paid-hours badge. Both Today and Tomorrow show two entries in compact and expanded layouts, including non-working days. Extra entries beyond two use a day-specific +N link. Launchers that report exact widget sizes get layouts based on those sizes, rather than a threshold that hides the second entry. Taps and existing theme/language/refresh behavior are retained.

New jobs start with an unused color. Colors belonging to other jobs (including archived jobs) are disabled in the editor and rejected at Save. Editing a job can retain its own unique color. Legacy duplicated colors are preserved until edited; such an edit requires choosing a free color. A larger palette and generated spare keep adding jobs possible when standard colors are occupied.

Validated application source: 3e775a1cac56a1475dcf3164ff802998e93e81bb.

GitHub Actions 37372673160: Android debug and release unit suites each passed 111 tests; debug/release lint, debug APK, signed Play AAB and CI jarsigner verification passed. Backend passed 17 tests and the generated 507-key / 8-language catalog check. UI validation 37372673070 passed all 38 tests, including disabled job colors and actual RemoteViews renders at compact/default/expanded sizes in English/Macedonian and Light/Dark, enlarged fonts, two entries per day, non-working days, overnight shifts and tap targets. Final PNGs were inspected; hours badge backgrounds have nonzero size and single-entry 250dp rows use the large design.

Signed AAB: PTS-0.2.13-23.aab; 29,550,709 bytes; SHA-256 d155b6ef196d0a0144fa44b7b8ff3484bd2987480ddf240f532c1c07a2807867. Upload certificate SHA-1 03:C2:26:1D:BF:1E:8E:65:99:41:15:06:32:9C:94:D8:AE:73:B8:BE matches the existing PTS upload key.

Play Alpha release 13: 0.2.13 · Widget design & job colors, version code 23 only, 100% of the existing Alpha track. Submitted on 2026-10-05; Publishing overview visibly confirms Changes in review with Google quick checks running. Managed publishing is off; approval remains pending. No devices lost; Play has the existing nonblocking native-debug-symbol warning.

Physical Samsung launcher pinning/resizing remains an owner check. Meta consent and update/download flow were not changed by this release.
