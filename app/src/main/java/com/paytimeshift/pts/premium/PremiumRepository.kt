package com.paytimeshift.pts.premium

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.domain.AppData
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

data class AccountStatus(val uid: String?=null,val email: String="",val verified: Boolean=false,
    val premium: Boolean=false,val expiresAt: Long=0,val monthlyEmail: Boolean=true,val yearlyEmail: Boolean=true,
    val backupAt: Long=0,val revision: Int=0,val testAccess: Boolean=false,val displayName: String="")
fun cloudBackupLabel(status:AccountStatus,bound:Boolean,resolved:Boolean,error:Boolean):String = when {
    !resolved && error -> "Cloud backup status unavailable. Refresh account."
    !resolved -> "Checking account…"
    !status.premium -> "Cloud backup paused. Premium is required."
    cloudRestoreRequired(status,bound) -> existingBackupMessage
    !bound -> "Choose cloud restore or enable backup on this phone first."
    status.backupAt==0L -> "Automatic backup is ready. No cloud backup saved yet."
    else -> "Automatic backup enabled on this phone"
}
fun cloudRestoreRequired(status:AccountStatus,bound:Boolean):Boolean = !bound && status.revision>0
const val existingBackupMessage="A cloud backup already exists. Restore it before enabling backup on this phone. Save a local backup first to keep your current data."
fun accountHash(uid: String)=MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString(""){"%02x".format(it)}
class PremiumRepository(val context: Context) {
    val auth=FirebaseAuth.getInstance()
    private val functions=FirebaseFunctions.getInstance("europe-west1")
    private val binding=context.getSharedPreferences("pts-cloud-device",Context.MODE_PRIVATE)
    suspend fun call(name: String,args: Map<String,Any> = emptyMap()): Map<*,*> = functions.getHttpsCallable(name).call(args).await().data as Map<*,*>
    companion object {
        // Retained within this process across activity recreation; never persisted or shared across UIDs.
        private val statusMutex=Mutex()
        @Volatile private var cached:AccountStatus?=null
        private var checkedAt=0L
        @Volatile private var cachedPreferences:List<Any>?=null
        private val preferenceMutex=Mutex()
        private val generation=java.util.concurrent.atomic.AtomicLong()
    }
    fun invalidateStatus() {generation.incrementAndGet();cached=null;cachedPreferences=null}
    suspend fun status(force:Boolean=false): AccountStatus = statusMutex.withLock {
        val uid=auth.currentUser?.uid
        if(uid==null) {invalidateStatus();return@withLock AccountStatus()}
        val now=android.os.SystemClock.elapsedRealtime()
        val hit=cached
        if(!force && hit?.uid==uid && StatusCachePolicy.fresh(now,checkedAt,System.currentTimeMillis(),hit.premium,hit.expiresAt)) return@withLock hit
        val requestedGeneration=generation.get()
        fetchStatus(force).also {if(auth.currentUser?.uid==uid && generation.get()==requestedGeneration) {cached=it;checkedAt=android.os.SystemClock.elapsedRealtime()}}
    }
    private suspend fun fetchStatus(force:Boolean): AccountStatus {
        val user=auth.currentUser ?: return AccountStatus()
        user.reload().await();user.getIdToken(force).await()
        val p=call("getAccountStatus",mapOf("force" to force));val ent=p["premium"] as? Map<*,*> ?: emptyMap<String,Any>()
        val expires=(ent["expiresAt"] as? Number)?.toLong() ?: 0
        return AccountStatus(user.uid,user.email ?: "",user.isEmailVerified,ent["active"]==true && expires>System.currentTimeMillis(),expires,
            p["monthlyEmail"]!=false,p["yearlyEmail"]!=false,(p["backupAt"] as? Number)?.toLong() ?: 0,(p["revision"] as? Number)?.toInt() ?: 0,ent["state"]=="PTS_TEST_ACCESS",user.displayName ?: "")
    }
    suspend fun verifyPurchase(token: String) {call("verifySubscription",mapOf("purchaseToken" to token));invalidateStatus()}
    suspend fun reportPreferences(monthly: Boolean,yearly: Boolean,language: String) = preferenceMutex.withLock {
        val uid=auth.currentUser?.uid ?: return@withLock
        val zone=java.time.ZoneId.systemDefault().id
        val key=listOf(uid,monthly,yearly,language,zone)
        if(cachedPreferences==key) return@withLock
        call("saveReportPreferences",mapOf("monthlyEmail" to monthly,"yearlyEmail" to yearly,"language" to language,"timeZone" to zone))
        if(auth.currentUser?.uid==uid) cachedPreferences=key
    }
    fun bound(uid: String)=binding.getString("uid",null)==uid
    fun bind(uid: String,revision: Int,digest: String?=null) {binding.edit().putString("uid",uid).putInt("revision",revision).putString("digest",digest).commit()}
    fun unbind() {binding.edit().clear().commit();invalidateStatus()}
    suspend fun backup(data: AppData) {
        val uid=auth.currentUser?.uid ?: error("Sign in first.")
        check(bound(uid)) {"Choose cloud restore or enable backup on this phone first."}
        val text=LocalStore.encode(data);val digest=accountHash(text)
        if(binding.getString("digest",null)==digest) return
        val revision=binding.getInt("revision",0)
        val result=call("saveCloudBackup",mapOf("text" to text,"expectedRevision" to revision))
        if(auth.currentUser?.uid==uid && bound(uid)) {bind(uid,(result["revision"] as Number).toInt(),digest);invalidateStatus()}
    }
    suspend fun restored(): Pair<AppData,Int> {
        val result=call("getCloudBackup");invalidateStatus();val data=LocalStore.decode(result["text"] as String)
        return data to (result["revision"] as Number).toInt()
    }
    suspend fun deleteAccount() {call("deletePtsAccount");unbind();auth.signOut()}
}
fun cloudError(e: Exception): String = when {
    e is IllegalStateException && e.message=="Restore existing backup first." -> existingBackupMessage
    e is IllegalStateException && e.message=="Choose cloud restore or enable backup on this phone first." -> "Choose cloud restore or enable backup on this phone first."
    e is IllegalStateException && e.message=="Sign in first." -> "Sign in before subscribing."
    e is IllegalArgumentException && e.message=="Choose the Google account with the same email address." -> "Choose the Google account with the same email address."
    e is androidx.credentials.exceptions.GetCredentialException -> "Google sign-in could not finish. Try again or use email."
    e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Check your email and password."
    e is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account already exists. Try Sign in."
    e is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Use a password with at least 8 characters."
    e is FirebaseFunctionsException && e.code==FirebaseFunctionsException.Code.ABORTED -> "A newer cloud backup exists. Restore it before uploading."
    e is FirebaseFunctionsException && e.code==FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Premium is required, or this purchase belongs to another account."
    e is FirebaseFunctionsException && e.code==FirebaseFunctionsException.Code.NOT_FOUND -> "No cloud backup exists."
    e is FirebaseFunctionsException && e.code==FirebaseFunctionsException.Code.FAILED_PRECONDITION -> "Verify your email or sign in again, then retry."
    else -> "Could not connect. Check your connection and try again."
}
