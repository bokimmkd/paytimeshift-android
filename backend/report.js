import PDFDocument from 'pdfkit';
import {analyze,monthBounds} from './analytics.js';
import Decimal from 'decimal.js';
import {fileURLToPath} from 'node:url';
import {readFileSync} from 'node:fs';
const font=fileURLToPath(new URL('./fonts/PTSReportSans.ttf',import.meta.url));
function translations(language){try {return JSON.parse(readFileSync(new URL('./translations.json',import.meta.url),'utf8'))[language] ?? {};} catch {return {};}}
const number=(value,language,places=2)=>Number(value).toLocaleString(language,{minimumFractionDigits:0,maximumFractionDigits:places});
export function reportPdf(data,year,month,annual,language='en'){
  const doc=new PDFDocument({size:'A4',margin:36,bufferPages:true});const chunks=[];
  const finished=new Promise((resolve,reject)=>{doc.on('data',d=>chunks.push(d));doc.on('end',()=>resolve(Buffer.concat(chunks)));doc.on('error',reject);});
  const labels=translations(language);const tr=s=>labels[s] ?? s;
  doc.font(readFileSync(font));
  function text(label,value='',heading=false,literal=false){
    if(doc.y>755) doc.addPage();doc.fontSize(heading?13:10).fillColor(heading?'#00857c':'#071c43');
    doc.text((literal?label:tr(label))+(value ? `  ·  ${value}` : ''),{lineGap:3});doc.moveDown(heading?.45:.15);
  }
  const from=annual?`${year}-01-01`:monthBounds(year,month)[0],until=annual?`${year}-12-31`:monthBounds(year,month)[1];
  const cache=new Map();const report=analyze(data,from,until,cache);const previousMonth=new Date(Date.UTC(year,month-2,1));const previous=analyze(data,...monthBounds(previousMonth.getUTCFullYear(),previousMonth.getUTCMonth()+1),cache);
  const money=(v,c)=>`${c} ${number(v,language,new Intl.NumberFormat('en',{style:'currency',currency:c}).resolvedOptions().maximumFractionDigits)}`;
  const hour=v=>`${number(v/60,language)} ${tr('h')}`;
  const metrics=t=>{for(const [key,label] of [['shifts','Total shifts'],['worked','Worked hours'],['paid','Paid hours'],['regular','Regular hours'],['overtime','Overtime'],['night','Night hours'],['weekend','Weekend hours'],['holiday','Holiday hours']]) text(label,key==='shifts'?String(t[key]):hour(t[key]));};
  text('PTS · Pay Time Shift','',true,true);text(annual?'Annual Work & Earnings Report':'Monthly Work & Earnings Report','',true);
  text('Period',`${from} – ${until}`);text('Estimated real earnings after work-related costs.');text('Taxes, government deductions and payroll deductions are not included.');
  if(!report.length) text('No shifts added');
  for(const c of report){
    text(c.currency,'',true,true);metrics(c.time);
    text('Gross estimated earnings',money(c.gross,c.currency));text('Work-related costs',money(c.costs,c.currency));text('Real estimated earnings',money(c.real,c.currency),true);
    text('Gross hourly value',c.time.worked?`${money(c.gross.times(60).div(c.time.worked),c.currency)} / ${tr('h')}`:'—');
    text('Real hourly value',c.time.worked?`${money(c.real.times(60).div(c.time.worked),c.currency)} / ${tr('h')}`:'—');
    text('Cost breakdown','',true);for(const [category,cost] of Object.entries(c.categories)) text(category,`${money(cost,c.currency)} · ${c.costs.gt(0)?number(cost.times(100).div(c.costs),language,1):0}%`);
    text('Breakdown by job','',true);for(const j of c.jobs){text(j.name,'',true,true);metrics(j.time);text('Gross estimated earnings',money(j.gross,c.currency));text('Work-related costs',money(j.costs,c.currency));text('Real estimated earnings',money(j.real,c.currency));text('Gross hourly value',j.grossHourly?money(j.grossHourly,c.currency)+' / '+tr('h'):'—');text('Real hourly value',j.realHourly?money(j.realHourly,c.currency)+' / '+tr('h'):'—');}
    if(annual){
      text('Average monthly real earnings',money(c.real.div(12),c.currency));text('Monthly trend','',true);
      const entries=[];for(let m=1;m<=12;m++){const t=analyze(data,...monthBounds(year,m),cache).find(t=>t.currency===c.currency);entries.push({month:m,present:!!t,...(t ?? {gross:new Decimal(0),costs:new Decimal(0),real:new Decimal(0),time:{worked:0,overtime:0,shifts:0}})});
        text(new Intl.DateTimeFormat(language,{month:'long',timeZone:'UTC'}).format(new Date(Date.UTC(year,m-1,1))),hour(t?.time.worked ?? 0),true,true);
        text('Gross estimated earnings',money(t?.gross ?? 0,c.currency));text('Work-related costs',money(t?.costs ?? 0,c.currency));text('Real estimated earnings',money(t?.real ?? 0,c.currency));}
      text('Highlights','',true);const active=entries.filter(t=>t.present);
      const monthName=m=>new Intl.DateTimeFormat(language,{month:'long',timeZone:'UTC'}).format(new Date(Date.UTC(year,m-1,1)));
      function high(label,key,highest=true){const sorted=[...active].sort((a,b)=>(highest?-1:1)*(Number(a[key])-Number(b[key])));if(sorted.length) text(label,monthName(sorted[0].month));}
      high('Highest earning month','real');high('Lowest earning month','real',false);high('Month with highest work costs','costs');
      const ot=active.toSorted((a,b)=>b.time.overtime-a.time.overtime)[0];if(ot) text('Month with most overtime',monthName(ot.month));
      for(const [key,label] of [['gross','Job with highest gross earnings'],['real','Job with highest real earnings'],['realHourly','Job with highest real hourly value']]) {const j=c.jobs.filter(j=>j[key]!==null).toSorted((a,b)=>Number(b[key])-Number(a[key]))[0];if(j) text(label,j.name);}
    }else{
      const p=previous.find(v=>v.currency===c.currency);text('Compared with previous month','',true);
      function diff(label,value,old){text(label,old===undefined || Number(old)===0?'—':`${Number(value)>=Number(old)?'↑':'↓'} ${number(new Decimal(value).minus(old).times(100).div(new Decimal(old).abs()),language,1)}%`);}
      diff('Worked hours',c.time.worked,p?.time.worked);diff('Gross estimated earnings',c.gross,p?.gross);diff('Work-related costs',c.costs,p?.costs);diff('Real estimated earnings',c.real,p?.real);
      if(c.time.worked) diff('Real hourly value',c.real.times(60).div(c.time.worked),p?.time.worked?p.real.times(60).div(p.time.worked):undefined);
    }
  }
  text('Night, weekend and holiday hours overlap other hours.');text('Weekly: once per workweek, on its first workday. Monthly: once per working month. Current cost rules apply to report history.');text('Different currencies are shown separately.');
  const pages=doc.bufferedPageRange();for(let i=0;i<pages.count;i++){doc.switchToPage(i);doc.fontSize(8).fillColor('#6b778b').text(`PTS · Pay Time Shift | ${i+1}/${pages.count}`,36,810,{lineBreak:false});}
  doc.end();return finished;
}
