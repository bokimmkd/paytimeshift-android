import {mkdir,writeFile} from 'node:fs/promises';
import {reportPdf} from '../report.js';
const job={id:'j',name:'Regular Job · Редовна работа',rate:'180',currency:'MKD',rules:{},costs:[{id:'fuel',name:'',category:'Fuel',amount:'300',frequency:'Per workday',enabled:true},{id:'food',name:'',category:'Food / Meals at work',amount:'100',frequency:'Per shift',enabled:true}]};
const data={schemaVersion:1,preferences:{language:'en'},jobs:[job],shifts:[]};
for(let month=1;month<=12;month++)for(let day=1;day<=20;day++) data.shifts.push({id:`${month}-${day}`,jobId:'j',date:`2026-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`,start:'07:00',end:'16:00',breakMinutes:30,paidBreak:false,rate:'180',currency:'MKD',fixedPay:false,kind:'Work',rules:{useHourlyRates:true,overtimeRate:'250',saturdayRate:'220',sundayRate:'250',nightPercent:10},bonus:'0'});
await mkdir('sample-reports',{recursive:true});
for(const language of ['en','mk','de','it','es','fr','sr','pt-BR','el']) await writeFile(`sample-reports/PTS-${language}-${language==='en'?'annual':'monthly'}.pdf`,await reportPdf(data,2026,10,language==='en',language));


const monthly={...data,jobs:[{...job,name:'Monthly Salary · Месечна плата',monthlyPay:true,salaryPeriods:[{from:'2026-01-01',until:'',amount:'40000',currency:'MKD'}]}],shifts:data.shifts.map(s=>({...s,monthlyPay:true})),adjustments:[{id:'bonus',jobId:'j',month:'2026-10',type:'Bonus',amount:'2000',currency:'MKD',reason:'Performance',note:'Monthly bonus'},{id:'deduction',jobId:'j',month:'2026-10',type:'Deduction',amount:'500',currency:'MKD',reason:'Lateness',note:'Manually entered'}]};
await writeFile('sample-reports/PTS-monthly-salary-mk.pdf',await reportPdf(monthly,2026,10,false,'mk'));
await writeFile('sample-reports/PTS-monthly-salary-annual-en.pdf',await reportPdf(monthly,2026,10,true,'en'));
