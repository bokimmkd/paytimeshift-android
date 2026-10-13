#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
npm ci --prefix backend --ignore-scripts --no-audit --no-fund
npm --prefix backend run check
npm --prefix backend test
npx --yes firebase-tools@14.17.0 deploy --project pts-pay-time-shift --only 'functions:pts-premium:getAccountStatus,functions:pts-premium:verifySubscription,functions:pts-premium:saveReportPreferences,functions:pts-premium:saveCloudBackup,functions:pts-premium:getCloudBackup,functions:pts-premium:deletePtsAccount,functions:pts-premium:playSubscriptionNotifications,functions:pts-premium:purgeAbuseLimits'
