// Run once with Admin/ADC credentials before deploying the due-only scheduler. No user data is deleted.
import {initializeApp} from 'firebase-admin/app';
import {getFirestore} from 'firebase-admin/firestore';
import {reportScheduleFields} from '../cloud-policy.js';
const args=process.argv.slice(2),apply=args.includes('--apply'),i=args.indexOf('--project');
if(i<0 || args[i+1]!=='pts-pay-time-shift') throw Error('Pass --project pts-pay-time-shift explicitly.');
initializeApp({projectId:args[i+1]});const db=getFirestore();let cursor,count=0;
do {
  let q=db.collection('users').orderBy('__name__').limit(100);if(cursor) q=q.startAfter(cursor);
  const page=await q.get();if(page.empty) break;
  for(const snap of page.docs) if(snap.data().reportScheduleVersion!==1) {
    if(apply) await db.runTransaction(async tx=>{const latest=await tx.get(snap.ref);if(latest.exists && latest.data().reportScheduleVersion!==1) tx.set(snap.ref,reportScheduleFields(latest.data()),{merge:true});});
    count++;
  }
  cursor=page.docs.at(-1);if(page.size<100) break;
}while(cursor);
console.log(`${apply?'Seeded':'Would seed'} ${count} legacy profiles. No private profile data printed.`);
