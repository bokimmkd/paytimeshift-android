# PTS 0.2.0 service activation and verification

Project: pts-pay-time-shift (924616791326), Android com.paytimeshift.pts. Region europe-west1. Work branch premium-analytics-upgrade. Preserve main/0.1.7 closed Alpha until integrations are verified. v10 is an Internal integration draft only. If Android code changes after this upload, the next uploaded build must use v11 or higher.

## Existing external state

Firebase Android/Web apps registered. Email/password enabled. Empty default Standard Firestore created in europe-west1, production rules deny all client reads/writes. Project remains Spark; paid Firebase Functions/Storage cannot be deployed until the owner completes Blaze activation.

Play Internal draft 0.2.0 · Premium integration draft contains signed version 10, replacing the earlier version 9 attachment. Subscription creation remains disabled in the current Console even after upload/save and refresh. Complete service setup and then activate a limited Internal test release to enable the catalog if required. Do not roll out an unverified upgrade to the closed Alpha groups.

## Required activation

1. Owner completes Blaze billing activation for this project. Reuse the intended billing account; do not open an unrelated financial account. A budget alert is an alert, not a spending cap.
2. Create default Storage bucket in europe-west1. Deploy deny-all Storage/Firestore rules from this repository. Do not enable public/test rules.
3. Enable Android Publisher API. Create the dedicated runtime service account pts-premium-runtime@pts-pay-time-shift.iam.gserviceaccount.com, already specified in setGlobalOptions.serviceAccount. It is a deployment prerequisite, not an existing credential. Grant only necessary Firestore/Storage/secret permissions. Grant Google Play purchase-management permissions scoped to PTS, not other apps or developer-account administrator permissions. Approval at action time is required for this new security access. Do not create/download long-lived service-account keys.
4. In an authenticated Firebase/Cloud Shell session, check out this branch, install backend dependencies, run checks/tests, and deploy to the explicit project. Example: `firebase deploy --project pts-pay-time-shift --only firestore:rules,storage,functions:pts-premium`. Do not use a different default project. First deployment may enable required Cloud Run/build/event APIs.
5. Configure PTS_RESEND_API_KEY in Secret Manager through the official secure secret flow. Set PTS_REPORT_SENDER to a real verified sender. Do not reuse another app's unverified sender or expose keys in repository/files/logs. Resend account/domain readiness must be checked before deployment; no sending is claimed until verified.
6. Configure topic pts-play-subscriptions and the Play RTDN topic name `projects/pts-pay-time-shift/topics/pts-play-subscriptions`. Grant the official Google Play notifications service identity publish access only to this topic. Grant/new access needs action-time approval. Send Console's RTDN test and verify delivery in the function logs without logging purchase tokens or recipient/pay data.
7. Create subscription pts_premium, base plan annual, P1Y auto-renew, United States USD 1.99. Product name PTS Premium · Buy us a coffee. Benefits: No Ads, Automatic Cloud Backup, Restore on new phone, Advanced Work Analytics, Detailed Monthly / Yearly Reports. Localize product text and market prices; show the actual Play price to users. Active product is required to test query/buy/restore; no fake local entitlement.
8. Publish account deletion/privacy pages with the upgrade, verify the URLs, and update Play declarations for account/email, cloud-backed financial/job information, purchase records, reports and SDK ad/device diagnostics. Declare purchases and ads for the new build. Keep 0.1.7's local-only disclosures accurate until replacement release.
9. Link PTS in AdMob and configure the applicable consent message. Verify test ads in debug, release banner with consent, and no ad requests for a verified Premium account. Do not modify other apps.

## End-to-end release gate

Use a Google Play license tester for purchases. Do not charge a real card for a QA purchase. Verify purchase, acknowledgement, renewal, cancellation until expiry, expiry/hold/pending lockout, restore after reinstall, account ownership mismatch and RTDN. Verify empty new install, existing 0.1.7 data migration, manual backup including costs, cloud save/change/restore on a second phone, revision conflict, empty install protection and account deletion. Verify verified-email delivery of monthly/yearly PDFs and preference OFF, timezone boundary and retry/idempotency. Check native PDF View/Save/Share/Print/Email and all nine languages. Compare native/server report totals on identical backup data.

Automatic emails use the last successful cloud backup; both switches default ON but only Premium verified emails receive reports. Costs use current cost rules for historical estimates. Per-week charges are assigned to the first workday of the ISO week, across month/year boundaries. Worked hours exclude all breaks; paid hours include paid breaks. Different currencies never aggregate. Time classifications overlap. Ties in highlights choose the first chronological month/job.

Only after the gate passes, complete the Internal integration draft with the verified final bundle (use a higher versionCode if Android code changes), publish to the existing three closed Alpha tester groups, and verify serving status. No production publication is authorized by this upgrade workflow.
