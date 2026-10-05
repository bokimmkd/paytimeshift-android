# PTS 0.2.12 (22) — Today and tomorrow widget

Based on 0.2.11 (21), commit a2508bf2ae4181f5908f89f57d2603e0ee69284c, premium-analytics-upgrade.

- Rounded PTS widget with a small clock logo, separate Today / Tomorrow panels, job colors, times and paid duration in expanded sizes.
- Vertical and horizontal resizing: compact below 220dp, expanded from 260×220dp, two rows per day from 260×320dp. Additional shifts use +N; taps open the selected shift or calendar day. Archived jobs are excluded. Overnight work stays on the start date with +1 beside its end time.
- Settings > Android widget offers only Activate widget when the launcher supports Android pinning. The launcher confirms addition. No instructions or extra first-launch promotion.
- Widget follows the app language, time format and appearance. Saved edits/restores refresh it; periodic and inexact next-day refresh update the dates. Android may delay background refresh while asleep.
- Existing Play update flow, optional Meta measurement and backup/billing rules retained.

Validation and Play submission are pending until recorded below. Physical home-screen launcher placement/resizing remains a tester check even after emulator rendering tests pass.
