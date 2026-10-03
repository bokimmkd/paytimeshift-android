package com.paytimeshift.pts.premium

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.paytimeshift.pts.data.LocalStore
import com.paytimeshift.pts.domain.AppData
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

data class AccountStatus(val uid: String?=null,val email: String="",val verified: Boolean=false,
    val premium: Boolean=false,val expiresAt: Long=0,val monthlyEmail: Boolean=true,val yearlyEmail: Boolean=true,
    val backupAt: Long=0,val revision: Int=0,val testAccess: Boolean=false,val displayName: String="")
fun accountHash(uid: String)=MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString(""){"%02x".format(it)}
class PremiumRepository(val context: Context) {
    val auth=FirebaseAuth.getInstance()
    private val functions=FirebaseFunctions.getInstance("europe-west1")
    private val binding=context.getSharedPreferences("pts-cloud-device",Context.MODE_PRIVATE)
    suspend fun call(name: String,args: Map<String,Any> = emptyMap()): Map<*,*> = functions.getHttpsCallable(name).call(args).await().data as Map<*,*>
    suspend fun status(): AccountStatus {
        val user=auth.currentUser ?: return AccountStatus()
        user.reload().await();user.getIdToken(true).await()
        val p=call("getAccountStatus");val ent=p["premium"] as? Map<*,*> ?: emptyMap<String,Any>()
        val expires=(ent["expiresAt"] as? Number)?.toLong() ?: 0
        return AccountStatus(user.uid,user.email ?: "",user.isEmailVerified,ent["active"]==true && expires>System.currentTimeMillis(),expires,
            p["monthlyEmail"]!=false,p["yearlyEmail"]!=false,(p["backupAt"] as? Number)?.toLong() ?: 0,(p["revision"] as? Number)?.toInt() ?: 0,ent["state"]=="PTS_TEST_ACCESS",user.displayName ?: "")
    }
    suspend fun verifyPurchase(token: String) {call("verifySubscription",mapOf("purchaseToken" to token))}
    suspend fun reportPreferences(monthly: Boolean,yearly: Boolean,language: String) {
        call("saveReportPreferences",mapOf("monthlyEmail" to monthly,"yearlyEmail" to yearly,"language" to language,"timeZone" to java.time.ZoneId.systemDefault().id))
    }
    fun bound(uid: String)=binding.getString("uid",null)==uid
    fun bind(uid: String,revision: Int,digest: String?=null) {binding.edit().putString("uid",uid).putInt("revision",revision).putString("digest",digest).commit()}
    fun unbind() {binding.edit().clear().commit()}
    suspend fun backup(data: AppData) {
        val uid=auth.currentUser?.uid ?: error("Sign in first.")
        check(bound(uid)) {"Choose cloud restore or enable backup on this phone first."}
        val text=LocalStore.encode(data);val digest=accountHash(text)
        if(binding.getString("digest",null)==digest) return
        val revision=binding.getInt("revision",0)
        val result=call("saveCloudBackup",mapOf("text" to text,"expectedRevision" to revision))
        if(auth.currentUser?.uid==uid && bound(uid)) bind(uid,(result["revision"] as Number).toInt(),digest)
    }
    suspend fun restored(): Pair<AppData,Int> {
        val result=call("getCloudBackup");val data=LocalStore.decode(result["text"] as String)
        return data to (result["revision"] as Number).toInt()
    }
    suspend fun deleteAccount() {call("deletePtsAccount");unbind();auth.signOut()}
}
fun cloudError(e: Exception): String = when {
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
