import test from 'node:test';
import assert from 'node:assert/strict';
import {freshEntitlement,duePeriods,nextReportAt,reportScheduleFields} from '../cloud-policy.js';
const now=Date.parse('2026-10-13T12:00:00Z');
const base={timeZone:'Europe/Skopje',backupObject:'backup.json',premium:{active:true,expiresAt:Date.parse('2027-12-01T00:00:00Z'),state:'SUBSCRIPTION_STATE_ACTIVE'},monthlyEmail:true,yearlyEmail:true};
test('server cache needs trusted matching token, recent verification and unexpired paid state',()=>{
 const p={...base,verifiedTokenHash:'token',verifiedAt:now};
 assert.equal(freshEntitlement(p,'token',now),p.premium);
 for(const bad of [{verifiedTokenHash:'other'},{verifiedAt:now-300000},{verifiedAt:now+1},{premium:{...base.premium,expiresAt:now}},{premium:{...base.premium,state:'SUBSCRIPTION_STATE_ON_HOLD'}},{premium:{...base.premium,active:false}}]) assert.equal(freshEntitlement({...p,...bad},'token',now),null);
 assert.ok(freshEntitlement({...p,premium:{...base.premium,state:'SUBSCRIPTION_STATE_CANCELED'}},'token',now));
});
test('free, expired, missing backup and disabled email profiles have no due queue item',()=>{
 for(const p of [{...base,premium:{active:false}},{...base,premium:{...base.premium,expiresAt:now}},{...base,backupObject:null},{...base,monthlyEmail:false,yearlyEmail:false}]) assert.equal(nextReportAt(p,now),null);
});
test('monthly report is scheduled once at 06:00 local time with DST conversion',()=>{
 assert.equal(nextReportAt(base,now),Date.parse('2026-11-01T05:00:00Z'));
 assert.equal(nextReportAt({...base,timeZone:'Pacific/Kiritimati'},now),Date.parse('2026-10-31T16:00:00Z'));
 assert.equal(nextReportAt({...base,timeZone:'America/Los_Angeles'},now),Date.parse('2026-11-01T14:00:00Z'));
});
test('January sends previous December and previous year and honours separate options',()=>{
 const jan=Date.parse('2027-01-01T12:00:00Z');
 assert.deepEqual(duePeriods(base,jan).map(p=>p.key),['month-2026-12','year-2026']);
 assert.deepEqual(duePeriods({...base,monthlyEmail:false},jan).map(p=>p.key),['year-2026']);
 assert.deepEqual(duePeriods({...base,lastMonthReport:'month-2026-12',lastYearReport:'year-2026'},jan),[]);
});
test('migration on first local day queues unsent reports, already sent profiles advance',()=>{
 const first=Date.parse('2026-10-01T12:00:00Z');
 assert.equal(reportScheduleFields(base,first).nextReportAt,first);
 assert.equal(nextReportAt({...base,lastMonthReport:'month-2026-9'},first),Date.parse('2026-11-01T05:00:00Z'));
});
test('annual-only profiles skip intervening months and expiry is never ignored',()=>{
 assert.equal(nextReportAt({...base,monthlyEmail:false},now),Date.parse('2027-01-01T05:00:00Z'));
 assert.equal(nextReportAt({...base,premium:{...base.premium,expiresAt:now+10000}},now),now+10000);
});
test('first-of-month boundaries respect UTC+14 and UTC-12',()=>{
 const boundary=Date.parse('2026-10-31T12:00:00Z');
 assert.deepEqual(duePeriods({...base,timeZone:'Pacific/Kiritimati'},boundary).map(p=>p.key),['month-2026-10']);
 assert.deepEqual(duePeriods({...base,timeZone:'Etc/GMT+12'},boundary),[]);
});
