import Decimal from 'decimal.js';
Decimal.set({precision:80,rounding:Decimal.ROUND_HALF_UP});
const D=v=>new Decimal(v ?? 0);
const date=s=>new Date(`${s}T00:00:00.000Z`);
const day=d=>d.toISOString().slice(0,10);
const minute=(d,t)=>new Date(`${d}T${t}:00.000Z`);
const monday=d=>{const n=date(d);n.setUTCDate(n.getUTCDate()-((n.getUTCDay()+6)%7));return day(n);};
export const categories=['Transport / Commute','Fuel','Parking & Tolls','Food / Meals at work','Work Clothing & Equipment','Phone / Mobile Data','Other'];
export const frequencies=['Per shift','Per workday','Weekly','Monthly'];
function decimalValid(v){try {return typeof v==='string' && v.length<=32 && D(v).isFinite() && D(v).gte(0);} catch {return false;}}
export function validateBackup(text){
  if(typeof text!=='string' || Buffer.byteLength(text)>4*1024*1024) throw Error('Invalid backup size');
  const d=JSON.parse(text);
  if(d.schemaVersion!==1 || !Array.isArray(d.jobs) || !Array.isArray(d.shifts) || d.jobs.length>1000 || d.shifts.length>30000 || !d.preferences) throw Error('Invalid backup');
  const ids=new Set();const sids=new Set();
  for(const j of d.jobs){
    if(typeof j.id!=='string' || !j.id || ids.has(j.id) || typeof j.name!=='string' || j.name.length>240 || !/^[A-Z]{3}$/.test(j.currency) || !decimalValid(j.rate)) throw Error('Invalid job');
    ids.add(j.id);validateRules(j.rules);
    if(j.costs!==undefined && (!Array.isArray(j.costs) || j.costs.length>100)) throw Error('Invalid costs');
    const costs=new Set();
    for(const c of j.costs ?? []) {if(typeof c.id!=='string' || !c.id || costs.has(c.id) || !categories.includes(c.category) || !frequencies.includes(c.frequency) || !decimalValid(c.amount) || typeof c.enabled!=='boolean' || typeof c.name!=='string' || c.name.length>120) throw Error('Invalid cost');costs.add(c.id);}
  }
  for(const s of d.shifts){
    if(!ids.has(s.jobId) || !['Work','Off','Vacation','Sick'].includes(s.kind ?? 'Work') || typeof s.fixedPay!=='boolean' || (s.paidBreak!==undefined && typeof s.paidBreak!=='boolean') || typeof s.id!=='string' || sids.has(s.id) || !validDate(s.date) || !validTime(s.start) || !validTime(s.end) || !decimalValid(s.rate) || !/^[A-Z]{3}$/.test(s.currency) || (s.bonus!==undefined && !decimalValid(s.bonus))) throw Error('Invalid shift');
    sids.add(s.id);validateRules(s.rules);
    const duration=times(s).duration;
    if(!Number.isInteger(s.breakMinutes) || s.breakMinutes<0 || s.breakMinutes>=duration) throw Error('Invalid break');
  }
  return d;
}
function validDate(s){return typeof s==='string' && /^\d{4}-\d{2}-\d{2}$/.test(s) && !isNaN(date(s)) && day(date(s))===s;}
function validTime(s){return typeof s==='string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(s);}
function validateRules(r){if(!r) return;for(const key of ['overtimeAfterHours','overtimePercent','nightPercent','sundayPercent']) if(r[key]!==undefined && (!Number.isFinite(r[key]) || r[key]<0 || r[key]>10000)) throw Error('Invalid rules');for(const key of ['overtimeRate','saturdayRate','sundayRate','holidayRate']) if(r[key] && !decimalValid(r[key])) throw Error('Invalid hourly price');for(const key of ['nightStart','nightEnd']) if(r[key] && !validTime(r[key])) throw Error('Invalid night time');if(r.holidayDates && (!Array.isArray(r.holidayDates) || r.holidayDates.some(d=>!validDate(d)))) throw Error('Invalid holiday');}
function times(s){const start=minute(s.date,s.start);let end=minute(s.date,s.end);if(end<=start) end=new Date(+end+86400000);const duration=(end-start)/60000;return {start,end,duration,paid:duration-(s.paidBreak?0:s.breakMinutes)};}
const emptyTime=()=>({shifts:0,worked:0,paid:0,regular:0,overtime:0,night:0,weekend:0,holiday:0});
export function shiftValues(s){
  if((s.kind ?? 'Work')!=='Work') return {gross:D(0),time:emptyTime()};
  const {start,end,duration,paid}=times(s);const r=s.rules ?? {};const proportion=paid/duration;
  const ot=Math.max(0,paid-(r.overtimeAfterHours ?? 8)*60);const t=emptyTime();Object.assign(t,{shifts:1,worked:duration-s.breakMinutes,paid,regular:paid-ot,overtime:ot});
  let gross=D(0);let night=0;let sunday=0;
  for(let at=new Date(start);at<end;at=new Date(+at+60000)){
    const clock=at.toISOString().slice(11,16);const ns=r.nightStart ?? '22:00',ne=r.nightEnd ?? '06:00';
    if(ns>ne ? clock>=ns || clock<ne : clock>=ns && clock<ne) night++;
    const dow=at.getUTCDay();if(dow===0) sunday++;
    if(dow===0 || dow===6) t.weekend+=proportion;
    const holiday=(r.holidayDates ?? []).includes(day(at));if(holiday) t.holiday+=proportion;

  }
  t.night=night*proportion;
  const rounded=v=>v.toDecimalPlaces(8,Decimal.ROUND_HALF_UP);
  const addition=(minutes,percent)=>rounded(D(s.rate).times(D(String(minutes))).times(percent).div(6000));
  if(s.fixedPay) gross=D(s.rate);
  else {
    if(r.useHourlyRates){
      const grouped=new Map();let at=new Date(start);let paidSoFar=0;
      const add=(label,minutes,price)=>{if(minutes<=1e-9) return;const key=label+':'+price.toString();const prior=grouped.get(key);grouped.set(key,{price,minutes:(prior?.minutes ?? 0)+minutes});};
      while(at<end){
        const midnight=new Date(Date.UTC(at.getUTCFullYear(),at.getUTCMonth(),at.getUTCDate()+1));const until=new Date(Math.min(+midnight,+end));
        const minutes=(until-at)/60000*proportion;const dow=at.getUTCDay();const holiday=(r.holidayDates ?? []).includes(day(at));
        const label=holiday?'Holiday work':dow===0?'Sunday work':dow===6?'Saturday work':'Regular hours';
        const price=D(holiday?r.holidayRate || s.rate:dow===0?r.sundayRate || s.rate:dow===6?r.saturdayRate || s.rate:s.rate);
        const normal=Math.max(0,Math.min(minutes,(r.overtimeAfterHours ?? 8)*60-paidSoFar));
        add(label,normal,price);add('Overtime',minutes-normal,r.overtimeRate?Decimal.max(price,D(r.overtimeRate)):price);
        paidSoFar+=minutes;at=until;
      }
      for(const line of grouped.values()) gross=gross.plus(rounded(line.price.times(D(String(line.minutes))).div(60)));
    }else {
      gross=rounded(D(s.rate).times(paid).div(60));
      if((r.overtimePercent ?? 0)>0 && ot>0) gross=gross.plus(addition(ot,r.overtimePercent));
      if((r.sundayPercent ?? 0)>0 && sunday>0) gross=gross.plus(addition(sunday*proportion,r.sundayPercent));
    }
    if((r.nightPercent ?? 0)>0 && night>0) gross=gross.plus(addition(night*proportion,r.nightPercent));
  }
  return {gross:gross.plus(D(s.bonus ?? 0)),time:t};
}
function addTime(a,b){for(const key of Object.keys(a)) a[key]+=b[key];return a;}
export function analyze(data,from,until,cache=new Map()){
  const totals=new Map();
  for(const job of data.jobs){
    const all=data.shifts.filter(s=>s.jobId===job.id && (s.kind ?? 'Work')==='Work').sort((a,b)=>a.date.localeCompare(b.date));
    const rows=all.filter(s=>s.date>=from && s.date<=until);const dates=[...new Set(all.map(s=>s.date))];
    const first=key=>[...Map.groupBy(dates,key).values()].map(ds=>ds[0]);
    const weeks=first(monday);const months=first(d=>d.slice(0,7));const costs={};
    for(const c of job.costs ?? []){if(!c.enabled) continue;const count=c.frequency==='Per shift' ? rows.length : (c.frequency==='Per workday' ? dates : c.frequency==='Weekly' ? weeks : months).filter(d=>d>=from && d<=until).length;costs[c.category]=D(costs[c.category] ?? 0).plus(D(c.amount).times(count));}
    for(const currency of new Set([...rows.map(s=>s.currency),...(Object.values(costs).some(c=>c.gt(0)) ? [job.currency] : [])])){
      const item={id:job.id,name:job.name,currency,gross:D(0),costs:D(0),categories:currency===job.currency ? costs : {},time:emptyTime()};
      for(const s of rows.filter(s=>s.currency===currency)){const value=cache.get(s) ?? shiftValues(s);cache.set(s,value);item.gross=item.gross.plus(value.gross);addTime(item.time,value.time);}
      for(const cost of Object.values(item.categories)) item.costs=item.costs.plus(cost);
      item.real=item.gross.minus(item.costs);item.grossHourly=item.time.worked ? item.gross.times(60).div(item.time.worked) : null;item.realHourly=item.time.worked ? item.real.times(60).div(item.time.worked) : null;
      if(!totals.has(currency)) totals.set(currency,{currency,gross:D(0),costs:D(0),real:D(0),time:emptyTime(),categories:{},jobs:[]});
      const total=totals.get(currency);total.jobs.push(item);total.gross=total.gross.plus(item.gross);total.costs=total.costs.plus(item.costs);total.real=total.gross.minus(total.costs);addTime(total.time,item.time);
      for(const [category,cost] of Object.entries(item.categories)) total.categories[category]=D(total.categories[category] ?? 0).plus(cost);
    }
  }
  return [...totals.values()].sort((a,b)=>a.currency.localeCompare(b.currency));
}
export function monthBounds(year,month){const start=new Date(Date.UTC(year,month-1,1));const end=new Date(Date.UTC(year,month,0));return [day(start),day(end)];}
