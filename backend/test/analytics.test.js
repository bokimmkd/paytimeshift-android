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


test('monthly salary is independent of month length and number of shifts; explicit extras only',()=>{
 const job={id:'j',name:'Monthly',currency:'MKD',rate:'180',monthlyPay:true,salaryPeriods:[{from:'2026-01-01',until:'',amount:'40000',currency:'MKD'}]};
 for(const m of [2,4,10]) for(const count of [0,1,20]) {
  const rows=Array.from({length:count},(_,i)=>shift(`2026-${String(m).padStart(2,'0')}-${String(i+1).padStart(2,'0')}`,{monthlyPay:true,rules:{useHourlyRates:true}}));
  const d={...data(rows),jobs:[job]};assert.equal(analyze(d,...monthBounds(2026,m))[0].gross.toString(),'40000');
 }
 const s=shift('2026-10-03',{end:'17:00',monthlyPay:true,rules:{useHourlyRates:true,saturdayRate:'20',overtimeRate:'100'}});
 assert.equal(shiftValues(s).gross.toString(),'360');assert.equal(shiftValues({...s,monthlyPay:false}).gross.toString(),'360');
});
test('monthly salary employment dates, archive history, adjustments and currency separation',()=>{
 const job={id:'j',name:'Salary',currency:'MKD',rate:'0',monthlyPay:true,archived:true,salaryPeriods:[{from:'2026-04-16',until:'2026-05-15',amount:'30000',currency:'MKD'}]};
 const d={...data([]),jobs:[job],adjustments:[{id:'b',jobId:'j',month:'2026-04',type:'Bonus',amount:'1000',currency:'MKD',reason:'Performance',note:''},{id:'d',jobId:'j',month:'2026-04',type:'Deduction',amount:'500',currency:'MKD',reason:'Lateness',note:''},{id:'eur',jobId:'j',month:'2026-05',type:'Bonus',amount:'5',currency:'EUR',reason:'Other',note:''}]};
 validateBackup(JSON.stringify(d));const april=analyze(d,...monthBounds(2026,4))[0];assert.equal(april.gross.toString(),'15000');assert.equal(april.adjusted.toString(),'15500');assert.equal(april.real.toString(),'15500');assert.equal(april.realHourly,null);
 assert.equal(analyze(d,...monthBounds(2026,3)).length,0);assert.equal(analyze(d,...monthBounds(2026,6)).length,0);
 assert.equal(analyze(d,'2026-04-01','2026-04-29')[0].monthlyBonuses.toString(),'0');
 const year=analyze(d,'2026-01-01','2026-12-31');assert.deepEqual(year.map(c=>c.currency),['EUR','MKD']);assert.equal(year[1].monthlyBonuses.toString(),'1000');assert.equal(year[1].monthlyDeductions.toString(),'500');
 for(const bad of [{...d,adjustments:[{...d.adjustments[0],amount:'-1'}]},{...d,adjustments:[{...d.adjustments[0],month:'2026-13'}]},{...d,jobs:[{...job,salaryPeriods:[{...job.salaryPeriods[0],until:'2026-01-01'}]}]}]) assert.throws(()=>validateBackup(JSON.stringify(bad)));
});
test('non-working day survives backups and never generates pay',()=>{
 const d=data([shift('2026-10-01',{kind:'Non-working day'})]);validateBackup(JSON.stringify(d));assert.equal(analyze(d,...monthBounds(2026,10)).length,0);
});
