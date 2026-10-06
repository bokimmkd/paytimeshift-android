# PTS service status — 2026-10-04

This live service record supplements the historical deployment notes. Android remains 0.2.4 (14); no Android rebuild or Play release upload was performed in this session.

## Source and Firebase
- Confirmed premium-analytics-upgrade HEAD before this record: 4e8899e6495d40756a987cfe05ffe6890e0aab05.
- Firebase project pts-pay-time-shift, region europe-west1, Blaze.
- Live Functions dashboard lists all eight core functions: verifySubscription, playSubscriptionNotifications, saveReportPreferences, saveCloudBackup, deletePtsAccount, getAccountStatus, getCloudBackup, purgeAbuseLimits.
- emailWorkReport and automaticReports are not deployed. Request counts are not proof of successful backup/restore or purchase QA.

## Google Play permissions — completed
- Owner explicitly approved the concrete PTS-only grant with “Da” after reviewing the prepared permissions.
- Invited pts-premium-runtime@pts-pay-time-shift.iam.gserviceaccount.com.
- Live Play user detail shows Active, access never expires, with only com.paytimeshift.pts listed.
- Four app permissions: View app information (read-only), its inherited View app quality information (read-only), View financial data, Manage orders and subscriptions.
- No developer-account-wide permission, administrator role, release permission, other-app grant or service-account private key was created.
- Proof: PTS-Play-access-active-1791098792610.jpg, Library libfile_6eb61a6f2c508191a0543e102d0d012c.
- Play API verification and acknowledgement still require a real license-test purchase; permission grant alone does not prove billing works.

## License testing — read-only verification
- BokiTESTER and StayGuide Internal TEST are selected in License testing; license response RESPOND_NORMALLY.
- BokiTESTER contains bokimk.ap@gmail.com.
- WarrantyCave Closed Test is not selected for license testing. No shared lists or license-test settings were changed.
- Existing server owner test access can already unlock Premium; do not count it as purchase proof. Confirm the app account can actually expose the purchase action for paid-entitlement QA.

## RTDN — still incomplete
- Live PTS Monetization setup initially had real-time notifications disabled and an empty topic.
- Prepared projects/pts-pay-time-shift/topics/pts-play-subscriptions and attempted synthetic test notifications; the transient sending message appeared, but no delivery result or function execution was verified.
- Discarded the unsaved draft; live RTDN remains disabled. Do not claim activation or successful delivery.
- Google Cloud topic console showed Site Unavailable after one reload.
- Firebase embedded Cloud Shell also showed Site Unavailable.
- Topic publisher IAM and Android Publisher API readiness remain unverified. No RTDN IAM grant was made.
- Next: regain Cloud access, inspect topic IAM, prepare any necessary topic-only Pub/Sub Publisher grant for the official Google Play identity for explicit approval, configure PTS RTDN and verify the synthetic test in execution logs without purchase tokens.

## Resend — current blocker
- Signed-in Resend Domains contains scancabinet.app (Verified), stayguide.mk (Not Started), warrantycave.com (Verified).
- paytimeshift.com is absent.
- Add domain displays “Upgrade to add new domains”: current plan domain limit reached, Pro offered at $20/month with 10 domains.
- No upgrade, charge, domain removal, API-key creation, sender substitution or changes to other apps were made.
- Owner must choose the email account/plan path before noreply@paytimeshift.com can be verified.
- PTS_RESEND_API_KEY secure setup, PTS_REPORT_SENDER, reply/support address bokimk.ap@gmail.com, report-only deployment and real verified-Premium report email delivery remain incomplete.

## Remaining device gate
License-test purchase/acknowledge/restore, expiry/pending/account mismatch and RTDN; cloud save/change/restore including salary, adjustments and day statuses; actual monthly/yearly PDF delivery, OFF preferences, timezone and retry/idempotency. Never charge a real card for QA. No production publication is authorized.
