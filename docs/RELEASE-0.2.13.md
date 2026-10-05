# PTS 0.2.13 (23) — Widget design and distinct job colors

Based on premium-analytics-upgrade, 6cd213dfaca3b216145f7ea2353e09cc0505c38a / application 0.2.12 (22).

The widget now follows the approved visual: a white/dark rounded surface, clock logo, separate muted dates, thin dividers, a colored stripe and briefcase circle for each job, and a matching paid-hours badge. Both Today and Tomorrow show two entries in compact and expanded layouts, including non-working days. Extra entries beyond two use a day-specific +N link. Launchers that report exact widget sizes get layouts based on those sizes, rather than a threshold that hides the second entry. Taps and existing theme/language/refresh behavior are retained.

New jobs start with an unused color. Colors belonging to other jobs (including archived jobs) are disabled in the editor and rejected at Save. Editing a job can retain its own unique color. Legacy duplicated colors are preserved until edited; such an edit requires choosing a free color. A larger palette and generated spare keep adding jobs possible when standard colors are occupied.

Validation and signed AAB / Alpha submission are pending. Physical Samsung launcher pinning/resizing remains an owner check.
