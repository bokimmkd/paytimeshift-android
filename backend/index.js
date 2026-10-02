import {initializeApp} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore,FieldValue} from 'firebase-admin/firestore';
import {getStorage} from 'firebase-admin/storage';
import {onCall,HttpsError} from 'firebase-functions/v2/https';
import {onSchedule} from 'firebase-functions/v2/scheduler';
import {onMessagePublished} from 'firebase-functions/v2/pubsub';
import {defineSecret,defineString} from 'firebase-functions/params';
import {setGlobalOptions} from 'firebase-functions/v2';
import {google} from 'googleapis';
import {createHash,randomUUID} from 'node:crypto';
import {validateBackup} from './analytics.js';
import {reportPdf} from './report.js';
import {Resend} from 'resend';

initializeApp();setGlobalOptions({region:'europe-west1',maxInstances:5,memory:'512MiB',timeoutSeconds:120});
const db=getFirestore(),auth=getAuth();const bucket=()=>getStorage().bucket();
const PACKAGE='com.paytimeshift.pts',PRODUCT='pts_premium',BASE_PLAN='annual';
const resendKey=defineSecret('PTS_RESEND_API_KEY');
const sender=defineString('PTS_REPORT_SENDER',{description:'Verified sender such as PTS <reports@your-verified-domain>'});
const hash=v=>createHash('sha256').update(v).digest('hex');
const profile=uid=>db.doc(`users/${uid}`);
const play=()=>google.androidpublisher({version:'v3',auth:new google.auth.GoogleAuth({scopes:['https://www.googleapis.com/auth/androidpublisher']})});
function identity(request){if(!request.auth) throw new HttpsError('unauthenticated','Sign in first.');return request.auth.uid;}
async function limited(uid,key,seconds=2){
  const ref=db.doc(`limits/${hash(uid+':'+key)}`);await db.runTransaction(async tx=>{const previous=await tx.get(ref);const now=Date.now();if(previous.exists && now-previous.data().at<seconds*1000) throw new HttpsError('resource-exhausted','Please wait and try again.');tx.set(ref,{at:now,expires:new Date(now+86400000)});});
}
function entitlement(subscription){
  const line=subscription.lineItems?.find(l=>l.productId===PRODUCT && l.offerDetails?.basePlanId===BASE_PLAN);
  if(!line) throw new HttpsError('invalid-argument','This is not the PTS annual subscription.');
  const expiresAt=Date.parse(line.expiryTime ?? '');
  const active=['SUBSCRIPTION_STATE_ACTIVE','SUBSCRIPTION_STATE_IN_GRACE_PERIOD','SUBSCRIPTION_STATE_CANCELED'].includes(subscription.subscriptionState) && Number.isFinite(expiresAt) && expiresAt>Date.now();
  return {active,expiresAt:Number.isFinite(expiresAt)?expiresAt:0,state:subscription.subscriptionState ?? 'UNKNOWN',autoRenew:line.autoRenewingPlan?.autoRenewEnabled===true};
}
async function verify(uid,token){
  if(typeof token!=='string' || token.length<10 || token.length>4096) throw new HttpsError('invalid-argument','Invalid purchase token.');
  const api=play();const {data}=await api.purchases.subscriptionsv2.get({packageName:PACKAGE,token});
  if(data.externalAccountIdentifiers?.obfuscatedExternalAccountId!==hash(uid)) throw new HttpsError('permission-denied','This purchase belongs to another account.');
  const next=entitlement(data);const ref=db.doc(`purchaseOwners/${hash(token)}`);
  await db.runTransaction(async tx=>{const owner=await tx.get(ref);if(owner.exists && owner.data().uid!==uid) throw new HttpsError('permission-denied','Purchase already linked.');
    tx.set(ref,{uid,token,updatedAt:Date.now()});tx.set(profile(uid),{premium:next,purchaseToken:token,verifiedAt:Date.now()},{merge:true});});
  if(next.active && data.acknowledgementState==='ACKNOWLEDGEMENT_STATE_PENDING') await api.purchases.subscriptions.acknowledge({packageName:PACKAGE,subscriptionId:PRODUCT,token,requestBody:{}});
  return next;
}
async function premium(uid){
  const snap=await profile(uid).get();const p=snap.data() ?? {};
  if(!p.purchaseToken) throw new HttpsError('permission-denied','Premium is required.');
  const ent=await verify(uid,p.purchaseToken);if(!ent.active) throw new HttpsError('permission-denied','Premium is not active.');return p;
}
export const getAccountStatus=onCall(async request=>{
  const uid=identity(request);await limited(uid,'status');const user=await auth.getUser(uid);
  await db.runTransaction(async tx=>{const ref=profile(uid),snap=await tx.get(ref);if(!snap.exists) tx.set(ref,{monthlyEmail:true,yearlyEmail:true,language:'en',timeZone:'UTC',createdAt:Date.now()});});
  let p=(await profile(uid).get()).data();let ent=p.premium ?? {active:false,expiresAt:0};
  if(p.purchaseToken) ent=await verify(uid,p.purchaseToken);
  return {premium:ent,email:user.email ?? '',emailVerified:user.emailVerified,monthlyEmail:p.monthlyEmail!==false,yearlyEmail:p.yearlyEmail!==false,backupAt:p.backupAt ?? 0,revision:p.revision ?? 0};
});
export const verifySubscription=onCall(async request=>{const uid=identity(request);await limited(uid,'purchase');return verify(uid,request.data?.purchaseToken);});
export const saveReportPreferences=onCall(async request=>{
  const uid=identity(request);const {monthlyEmail,yearlyEmail,language,timeZone}=request.data ?? {};
  if(typeof monthlyEmail!=='boolean' || typeof yearlyEmail!=='boolean' || !['en','mk','de','it','es','fr','sr','pt-BR','el'].includes(language)) throw new HttpsError('invalid-argument','Invalid report preferences.');
  try {new Intl.DateTimeFormat('en',{timeZone}).format(new Date());} catch {throw new HttpsError('invalid-argument','Invalid timezone.');}
  await profile(uid).set({monthlyEmail,yearlyEmail,language,timeZone},{merge:true});return {saved:true};
});
export const saveCloudBackup=onCall({timeoutSeconds:120},async request=>{
  const uid=identity(request);await limited(uid,'backup',10);await premium(uid);
  const {text,expectedRevision}=request.data ?? {};let data;
  try {data=validateBackup(text);} catch {throw new HttpsError('invalid-argument','Invalid backup data.');}
  if(!Number.isInteger(expectedRevision) || expectedRevision<0) throw new HttpsError('invalid-argument','Invalid backup revision.');
  const object=`backups/${uid}/${randomUUID()}.json`;await bucket().file(object).save(text,{contentType:'application/json',resumable:false,metadata:{cacheControl:'private,no-store'}});
  let old;let revision;
  try {await db.runTransaction(async tx=>{const ref=profile(uid),p=(await tx.get(ref)).data() ?? {};if((p.revision ?? 0)!==expectedRevision) throw new HttpsError('aborted','A newer cloud backup exists. Restore it before uploading.');
    // Empty installs must never silently overwrite populated cloud data.
    if(p.backupJobCount>0 && data.jobs.length===0) throw new HttpsError('failed-precondition','Empty data cannot replace this cloud backup.');
    old=p.previousBackupObject;revision=expectedRevision+1;tx.set(ref,{backupObject:object,previousBackupObject:p.backupObject ?? null,backupAt:Date.now(),revision,backupJobCount:data.jobs.length,backupShiftCount:data.shifts.length,backupDigest:hash(text)},{merge:true});});}
  catch(e){await bucket().file(object).delete({ignoreNotFound:true});throw e;}
  if(old) await bucket().file(old).delete({ignoreNotFound:true});return {revision,backupAt:Date.now()};
});
export const getCloudBackup=onCall(async request=>{
  const uid=identity(request);await limited(uid,'restore');await premium(uid);const p=(await profile(uid).get()).data();
  if(!p.backupObject) throw new HttpsError('not-found','No cloud backup exists.');const [bytes]=await bucket().file(p.backupObject).download();const text=bytes.toString('utf8');validateBackup(text);return {text,revision:p.revision,backupAt:p.backupAt};
});
export const deletePtsAccount=onCall(async request=>{
  const uid=identity(request);if(Date.now()/1000-Number(request.auth.token.auth_time)>300) throw new HttpsError('failed-precondition','Sign in again before deleting your account.');
  await bucket().deleteFiles({prefix:`backups/${uid}/`});
  const owners=await db.collection('purchaseOwners').where('uid','==',uid).get();
  const batch=db.batch();owners.docs.forEach(doc=>batch.delete(doc.ref));batch.delete(profile(uid));await batch.commit();await auth.deleteUser(uid);
  return {deleted:true};
});
export const playSubscriptionNotifications=onMessagePublished({topic:'pts-play-subscriptions',retry:true},async event=>{
  const message=event.data.message.json; if(message.packageName!==PACKAGE || !message.subscriptionNotification?.purchaseToken) return;
  const token=message.subscriptionNotification.purchaseToken;const owner=await db.doc(`purchaseOwners/${hash(token)}`).get();if(owner.exists) await verify(owner.data().uid,token);
});
async function sendReport(uid,year,month,annual,key){
  const user=await auth.getUser(uid);if(!user.emailVerified || !user.email) return;
  const p=await premium(uid);if(!p.backupObject) return;
  const [bytes]=await bucket().file(p.backupObject).download();const data=validateBackup(bytes.toString('utf8'));
  const pdf=await reportPdf(data,year,month,annual,p.language ?? 'en');
  const result=await new Resend(resendKey.value()).emails.send({from:sender.value(),to:user.email,
    subject:`PTS · ${annual?'Annual':'Monthly'} Work & Earnings Report — ${annual?year:`${year}-${String(month).padStart(2,'0')}`}`,
    text:'Your estimated work and earnings report is attached. Work-related costs are deducted; taxes and payroll deductions are not included. You can disable automatic reports in PTS Account settings.',
    attachments:[{filename:`PTS-${annual?year:`${year}-${month}`}-work-report.pdf`,content:pdf}]}, {idempotencyKey:key});
  if(result.error) throw Error('Report email delivery failed');return true;
}
export const emailWorkReport=onCall({secrets:[resendKey]},async request=>{
  const uid=identity(request);await limited(uid,'email',60);const {year,month,annual}=request.data ?? {};
  if(!Number.isInteger(year) || year<1900 || year>9999 || !Number.isInteger(month) || month<1 || month>12 || typeof annual!=='boolean') throw new HttpsError('invalid-argument','Invalid report period.');
  const sent=await sendReport(uid,year,month,annual,`pts-manual-${uid}-${randomUUID()}`);if(!sent) throw new HttpsError('failed-precondition','Verify your email and save a cloud backup first.');return {sent:true};
});
// Hourly timezone-aware scheduler retries failed sends on the first local calendar day.
export const automaticReports=onSchedule({schedule:'0 * * * *',timeZone:'UTC',secrets:[resendKey],retryCount:2,timeoutSeconds:540},async()=>{
  let cursor;
  do {
    let query=db.collection('users').where('premium.active','==',true).orderBy('__name__').limit(100);if(cursor) query=query.startAfter(cursor);
    const profiles=await query.get();if(profiles.empty) break;
    for(const snap of profiles.docs){
      const p=snap.data();const parts=new Intl.DateTimeFormat('en-US',{timeZone:p.timeZone ?? 'UTC',year:'numeric',month:'numeric',day:'numeric'}).formatToParts(new Date());
      const get=k=>Number(parts.find(v=>v.type===k).value);if(get('day')!==1) continue;
      const month=get('month')===1?12:get('month')-1;const year=get('month')===1?get('year')-1:get('year');
      for(const annual of get('month')===1 ? [false,true] : [false]) {
        const key=`${annual?'year':'month'}-${year}${annual?'':`-${month}`}`;const field=annual?'lastYearReport':'lastMonthReport';
        if((annual?p.yearlyEmail:p.monthlyEmail)===false || p[field]===key) continue;
        try {if(await sendReport(snap.id,year,month,annual,`pts-auto-${snap.id}-${key}`)) await snap.ref.set({[field]:key},{merge:true});}
        catch {console.error('PTS automatic report deferred',hash(snap.id).slice(0,12),key);}
      }
    }
    cursor=profiles.docs.at(-1);if(profiles.size<100) break;
  }while(cursor);
});
