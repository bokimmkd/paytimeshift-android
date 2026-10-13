package com.paytimeshift.pts.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.paytimeshift.pts.R
import kotlinx.coroutines.tasks.await
import java.util.UUID

private tailrec fun Context.activity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> error("Google sign-in requires an activity.")
}

/** Credentials are exchanged with Firebase, never stored in PTS preferences or backups. */
suspend fun continueWithGoogle(context: Context, auth: FirebaseAuth, link: Boolean = false) {
    val current = auth.currentUser
    val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id))
        .setNonce(UUID.randomUUID().toString()).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(context).getCredential(context.activity(), request).credential
    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
    val google = GoogleIdTokenCredential.createFrom(credential.data)
    val firebase = GoogleAuthProvider.getCredential(google.idToken, null)
    if (link) {
        check(current != null && auth.currentUser?.uid == current.uid)
        require(current.email.equals(google.id, ignoreCase = true)) { "Choose the Google account with the same email address." }
        current.linkWithCredential(firebase).await()
    } else {
        auth.signInWithCredential(firebase).await()
    }
}

suspend fun clearGoogleSession(context: Context) {
    // Firebase sign-out happens first. A provider cleanup failure must never retain Premium access.
    runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
}
