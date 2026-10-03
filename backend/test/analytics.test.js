import test from 'node:test';
import assert from 'node:assert/strict';
import {analyze,shiftValues,monthBounds,validateBackup} from '../analytics.js';
import {subscriptionEntitlement} from '../entitlement.js';
import {reportPdf} from '../report.js';
const shift=(date='2026-10-01',extra={})=>({id:date,jobId:'j',date,start:'07:00',end:'15:00',breakMinutes:0,paidBreak:false,rate:'180',currency:'MKD',fixedPay:false,kind:'Work',rules:{},...extra});
const cost=(frequency,amount='10',category='Fuel',enabled=true)=>({id:frequency,name:'',frequency,amount,category,enabled});
const data=(shifts,costs=[])=>({schemaVersion:1,preferences:{language:'en'},jobs:[{id:'j',name:'Job',rate:'180',currency:'MKD',rules:{},costs}],shifts});
test('gross engine remains separate from costs and distinct workdays',()=>{
 const s=[shift(),shift('2026-10-01',{id:'evening',start:'18:00',end:'20:00'}),shift('2026-10-02')];
 const c=[cost('Per shift'),cost('Per workday','20'),cost('Weekly','50'),cost('Monthly','100'),{...cost('Monthly','999','Other',false),id:'disabled'}];
 const t=analyze(data(s,c),...monthBounds(2026,10))[0];assert.equal(t.gross.toString(),'3240');assert.equal(t.costs.toString(),'220');assert.equal(t.real.toString(),'3020');assert.equal(t.time.shifts,3);
});
test('ISO weekly charge belongs to first workday, even across year boundaries',()=>{
 const d=data([shift('2026-12-31'),shift('2027-01-01')],[cost('Weekly','100')]);
 assert.equal(analyze(d,...monthBounds(2026,12))[0].costs.toString(),'100');assert.equal(analyze(d,...monthBounds(2027,1))[0].costs.toString(),'0');
});
test('saved shift currency and job-cost currency cannot merge',()=>{
 const d=data([shift('2026-10-01',{currency:'EUR',rate:'10'})],[cost('Monthly','20')]);const t=analyze(d,...monthBounds(2026,10));assert.deepEqual(t.map(r=>r.currency),['EUR','MKD']);assert.equal(t[0].real.toString(),'80');assert.equal(t[1].real.toString(),'-20');assert.equal(t[1].realHourly,null);
});
test('paid breaks contribute to paid, not worked, hours and classifications overlap',()=>{
 const s=shift('2026-10-31',{start:'22:00',end:'08:00',breakMinutes:30,paidBreak:true,rules:{useHourlyRates:true,saturdayRate:'200',sundayRate:'250',overtimeRate:'300',nightPercent:10}});
 const t=shiftValues(s);assert.equal(t.time.worked,570);assert.equal(t.time.paid,600);assert.equal(t.time.overtime,120);assert.equal(t.time.night,480);assert.equal(t.time.weekend,600);assert.equal(t.gross.toString(),'2644');
});
test('hourly price replaces weekend and uses higher overtime price',()=>{
 const s=shift('2026-10-04',{start:'07:00',end:'17:00',rate:'10',bonus:'5',rules:{useHourlyRates:true,sundayRate:'20',holidayRate:'30',holidayDates:['2026-10-04'],overtimeRate:'25'}});
 assert.equal(shiftValues(s).gross.toString(),'305');assert.equal(shiftValues(s).time.holiday,600);
});
test('legacy percentages stack, fixed shifts exclude percentage extras, days off exclude all pay',()=>{
 const s=shift('2026-10-04',{start:'22:00',end:'08:00',rate:'10',bonus:'5',rules:{overtimePercent:50,nightPercent:20,sundayPercent:10}});
 assert.equal(shiftValues(s).gross.toString(),'133');assert.equal(shiftValues({...s,fixedPay:true,rate:'55'}).gross.toString(),'60');assert.equal(shiftValues({...s,kind:'Vacation'}).gross.toString(),'0');
});
test('fractional duration rounds gross to the same eight decimals as Android',()=>{
 assert.equal(shiftValues(shift('2026-10-01',{end:'07:01',rate:'1'})).gross.toString(),'0.01666667');
 assert.equal(shiftValues(shift('2026-10-01',{end:'07:01',rate:'1',rules:{useHourlyRates:true}})).gross.toString(),'0.01666667');
});
test('old backups without costs stay valid; invalid costs and missing jobs rejected',()=>{
 const d=data([shift()]);delete d.jobs[0].costs;assert.equal(validateBackup(JSON.stringify(d)).jobs.length,1);
 d.jobs[0].costs=[cost('Monthly','-1')];assert.throws(()=>validateBackup(JSON.stringify(d)));
 d.jobs[0].costs=[];d.shifts[0].jobId='missing';assert.throws(()=>validateBackup(JSON.stringify(d)));
});
test('annual costs equal sum of monthly costs, including ISO-week allocation',()=>{
 const d=data([shift('2026-01-01'),shift('2026-01-30'),shift('2026-02-02')],[cost('Monthly','20'),cost('Weekly','10')]);
 const annual=analyze(d,'2026-01-01','2026-12-31')[0];let costs=0;let gross=0;for(let m=1;m<=12;m++){const c=analyze(d,...monthBounds(2026,m))[0];costs+=Number(c?.costs ?? 0);gross+=Number(c?.gross ?? 0);}assert.equal(Number(annual.costs),costs);assert.equal(Number(annual.gross),gross);
});
test('Google Play pending, hold, paused and expired never unlock Premium',()=>{
 const base={lineItems:[{productId:'pts_premium',offerDetails:{basePlanId:'annual'},expiryTime:'2027-01-01T00:00:00Z',autoRenewingPlan:{autoRenewEnabled:true}}]};const now=Date.parse('2026-10-03');
 for(const state of ['ACTIVE','IN_GRACE_PERIOD','CANCELED']) assert.equal(subscriptionEntitlement({...base,subscriptionState:'SUBSCRIPTION_STATE_'+state},now).active,true);
 for(const state of ['PENDING','ON_HOLD','PAUSED','EXPIRED','PENDING_PURCHASE_CANCELED']) assert.equal(subscriptionEntitlement({...base,subscriptionState:'SUBSCRIPTION_STATE_'+state},now).active,false);
 assert.equal(subscriptionEntitlement({...base,subscriptionState:'SUBSCRIPTION_STATE_ACTIVE'},Date.parse('2027-02-01')).active,false);assert.throws(()=>subscriptionEntitlement({lineItems:[{productId:'another_app'}]},now));
});
test('all report languages create real PDF documents including Cyrillic and Greek',async()=>{
 for(const language of ['en','mk','de','it','es','fr','sr','pt-BR','el']) {const pdf=await reportPdf(data([shift()],[cost('Monthly','50')]),2026,10,false,language);assert.equal(pdf.subarray(0,4).toString(),'%PDF');assert.ok(pdf.length>1000);}
});
