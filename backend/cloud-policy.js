const TTL=5*60*1000;
const paidStates=new Set(['SUBSCRIPTION_STATE_ACTIVE','SUBSCRIPTION_STATE_IN_GRACE_PERIOD','SUBSCRIPTION_STATE_CANCELED']);
export function freshEntitlement(profile,tokenHash,now=Date.now()) {
  const p=profile?.premium,checked=profile?.verifiedAt;
  return profile?.verifiedTokenHash===tokenHash && Number.isFinite(checked) && checked<=now && now-checked<TTL &&
    p?.active===true && paidStates.has(p.state) && Number.isFinite(p.expiresAt) && p.expiresAt>now ? p : null;
}

function parts(at,zone) {
  const format=new Intl.DateTimeFormat('en-US',{timeZone:zone,year:'numeric',month:'numeric',day:'numeric',hour:'numeric',minute:'numeric',second:'numeric',hourCycle:'h23'});
  const values=Object.fromEntries(format.formatToParts(new Date(at)).filter(p=>p.type!=='literal').map(p=>[p.type,Number(p.value)]));
  return values;
}
export function localMonth(p,now=Date.now()) {
  const v=parts(now,p.timeZone ?? 'UTC');
  return {year:v.year,month:v.month,day:v.day};
}
export function duePeriods(p,now=Date.now()) {
  const {year:y,month:m,day}=localMonth(p,now);
  if(day!==1) return [];
  const year=m===1?y-1:y,month=m===1?12:m-1;
  return (m===1?[false,true]:[false]).filter(annual=>(annual?p.yearlyEmail:p.monthlyEmail)!==false).map(annual=>({
    year,month,annual,key:`${annual?'year':'month'}-${year}${annual?'':`-${month}`}`,field:annual?'lastYearReport':'lastMonthReport'
  })).filter(period=>p[period.field]!==period.key);
}
function localInstant(year,month,zone) {
  // 06:00 on the first local day avoids the usual midnight/DST discontinuities.
  const target=Date.UTC(year,month-1,1,6);let guess=target;
  for(let i=0;i<5;i++) {const v=parts(guess,zone);const shown=Date.UTC(v.year,v.month-1,v.day,v.hour,v.minute,v.second);const delta=target-shown;if(delta===0) break;guess+=delta;}
  return guess;
}
export function nextReportAt(p,now=Date.now()) {
  if(p.premium?.active!==true || !Number.isFinite(p.premium.expiresAt) || p.premium.expiresAt<=now || !p.backupObject || p.monthlyEmail===false && p.yearlyEmail===false) return null;
  if(duePeriods(p,now).length) return now;
  const {year,month}=localMonth(p,now);
  const next=p.monthlyEmail!==false ? localInstant(month===12?year+1:year,month===12?1:month+1,p.timeZone ?? 'UTC') : localInstant(year+1,1,p.timeZone ?? 'UTC');
  return Math.min(next,p.premium.expiresAt);
}
export function reportScheduleFields(p,now=Date.now()) {return {reportScheduleVersion:1,nextReportAt:nextReportAt(p,now)};}
