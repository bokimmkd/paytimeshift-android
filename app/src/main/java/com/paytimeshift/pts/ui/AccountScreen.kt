package com.paytimeshift.pts.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.paytimeshift.pts.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.domain.AppData
import com.paytimeshift.pts.premium.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable fun AccountDialog(repo: PremiumRepository,status: AccountStatus,price: String?,refresh: ()->Unit,buy: ()->Unit,restorePurchase: ()->Unit,
    data: AppData,restoreData: suspend (AppData)->Unit,close: ()->Unit) {
    val scope=rememberCoroutineScope();val context=LocalContext.current
    val language=LocalLanguage.current
    LaunchedEffect(language) {repo.auth.setLanguageCode(language)}
    var email by remember {mutableStateOf("")};var password by remember {mutableStateOf("")};var signup by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)}
    var restoreConfirm by remember {mutableStateOf(false)};var deleteConfirm by remember {mutableStateOf(false)}
    var enableConfirm by remember {mutableStateOf(false)}
    fun task(block: suspend ()->Unit) {if(busy) return;busy=true;error=null;scope.launch {try {block();refresh()} catch(_: GetCredentialCancellationException) { /* User dismissed the chooser. */ } catch(e: Exception){error=cloudError(e)} finally {busy=false}}}
    BrandedEditor(onDismissRequest=close,title={ScreenHeading("Account & Premium",Icons.Outlined.PersonOutline,"Buy us a coffee ☕",compact=true)},error=error,
        text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item {FormSection("Premium") {
                UiText(if(status.premium) "Premium active" else "Buy us a coffee ☕",fontSize=19.sp,fontWeight=FontWeight.Bold)
                if(!status.premium) UiText(if(price!=null) "$price / ${translate("year",LocalLanguage.current)}" else "$1.99 / year",fontSize=17.sp,color=MaterialTheme.colorScheme.primary)
                listOf("No Ads","Automatic Cloud Backup","Restore on new phone","Advanced Work Analytics","Detailed Monthly / Yearly Reports").forEach {benefit->Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {Icon(Icons.Outlined.CheckCircleOutline,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary);UiText(benefit,fontSize=12.sp)}}
                if(status.premium && !status.testAccess) {
                    UiText("${translate("Access until",LocalLanguage.current)}: ${java.time.Instant.ofEpochMilli(status.expiresAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}",fontSize=11.sp)
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/account/subscriptions?sku=pts_premium&package=com.paytimeshift.pts")))}){UiText("Manage subscription",fontSize=12.sp)}
                } else if(!status.premium) {
                    Button(onClick=buy,enabled=status.uid!=null && price!=null && !busy,contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)) {Icon(Icons.Outlined.LocalCafe,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));UiText("Subscribe yearly",fontSize=12.sp)}
                    UiText("Renews yearly. Cancel anytime in Google Play. The price and terms shown by Google Play apply.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(price==null) UiText("Subscription is not available in Google Play yet. Please try again later.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(status.uid==null) UiText("Sign in before subscribing.",fontSize=11.sp,color=MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick=restorePurchase,enabled=status.uid!=null && !busy) {Icon(Icons.Outlined.Restore,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Restore purchases",fontSize=12.sp)}
            }}
            if(status.uid==null) item {FormSection(if(signup) "Create account" else "Sign in") {
                GoogleAccountButton("Continue with Google",!busy) {task {continueWithGoogle(context,repo.auth)}}
                UiText("Or use your email",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                CompactField("Email address",email,{email=it.trim()},KeyboardType.Email)
                OutlinedTextField(value=password,onValueChange={password=it},singleLine=true,modifier=Modifier.fillMaxWidth(),label={UiText("Password",fontSize=11.sp)},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
                FormPair(first={Button(enabled=!busy && email.isNotBlank() && password.length>=(if(signup) 8 else 6),onClick={task {
                    if(signup) {val user=repo.auth.createUserWithEmailAndPassword(email,password).await().user;user?.sendEmailVerification()?.await()} else repo.auth.signInWithEmailAndPassword(email,password).await()
                    password=""
                }},contentPadding=PaddingValues(8.dp)){UiText(if(signup) "Create account" else "Sign in",fontSize=12.sp)}},
                    second={TextButton(onClick={signup=!signup}){UiText(if(signup) "Sign in" else "Create account",fontSize=12.sp)}})
                TextButton(enabled=!busy && email.isNotBlank(),onClick={task {repo.auth.sendPasswordResetEmail(email).await();error="If an account exists, a reset email has been sent."}}){UiText("Forgot password?",fontSize=11.sp)}
                UiText("Your jobs remain on this phone. Signing in does not replace your local data.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }} else {
                item {FormSection("Profile") {
                    if(status.displayName.isNotBlank()) Text(status.displayName,fontSize=16.sp,fontWeight=FontWeight.Bold)
                    Text(status.email,fontSize=13.sp,fontWeight=FontWeight.Bold)
                    if(repo.auth.currentUser?.providerData?.none {it.providerId=="google.com"}==true)
                        GoogleAccountButton("Link Google account",!busy) {task {continueWithGoogle(context,repo.auth,link=true)}}
                    if(!status.verified) {
                        UiText("Verify your email to receive automatic reports.",fontSize=11.sp)
                        TextButton(enabled=!busy,onClick={task {repo.auth.currentUser?.sendEmailVerification()?.await();error="Verification email sent."}}){UiText("Send verification email",fontSize=11.sp)}
                    }
                    TextButton(onClick={task {repo.status()}},enabled=!busy){Icon(Icons.Outlined.Refresh,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Refresh account",fontSize=11.sp)}
                    TextButton(onClick={stopCloudBackup(context,status.uid);repo.unbind();repo.auth.signOut();refresh();scope.launch {clearGoogleSession(context)}},enabled=!busy){Icon(Icons.Outlined.Logout,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Sign out",fontSize=11.sp)}
                }}
                item {FormSection("Cloud backup") {
                    UiText(if(repo.bound(status.uid!!)) "Automatic backup enabled on this phone" else "Choose cloud restore or enable backup on this phone first.",fontSize=11.sp)
                    UiText("${translate("Last cloud backup",LocalLanguage.current)}: ${if(status.backupAt==0L) "—" else java.time.Instant.ofEpochMilli(status.backupAt).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().toString()}",fontSize=10.sp)
                    FormPair(first={OutlinedButton(enabled=status.premium && !busy,onClick={enableConfirm=true},contentPadding=PaddingValues(8.dp)){Icon(Icons.Outlined.CloudUpload,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Back up now",fontSize=11.sp)}},
                        second={OutlinedButton(enabled=status.premium && !busy,onClick={restoreConfirm=true},contentPadding=PaddingValues(8.dp)){Icon(Icons.Outlined.CloudDownload,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));UiText("Restore backup",fontSize=11.sp)}})
                    if(!status.premium) UiText("Premium is required.",fontSize=10.sp)
                }}
                item {FormSection("Automatic email reports") {
                    CompactToggle("Monthly Report Email",status.monthlyEmail,enabled=status.premium && !busy){value->task {repo.reportPreferences(value,status.yearlyEmail,data.preferences.language)}}
                    CompactToggle("Yearly Report Email",status.yearlyEmail,enabled=status.premium && !busy){value->task {repo.reportPreferences(status.monthlyEmail,value,data.preferences.language)}}
                    UiText("Sent at the start of the next month or year to your verified account email. Reports use your latest cloud backup.",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }}
                item {TextButton(onClick={deleteConfirm=true},enabled=!busy){Icon(Icons.Outlined.DeleteOutline,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.error);Spacer(Modifier.width(4.dp));UiText("Delete account",fontSize=12.sp,color=MaterialTheme.colorScheme.error)}}
            }
            item {if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())}
        }},confirmButton={TextButton(onClick=close){UiText("Close",fontSize=12.sp)}},dismissButton={})
    if(restoreConfirm) AlertDialog(onDismissRequest={restoreConfirm=false},title={ScreenHeading("Restore backup",Icons.Outlined.CloudDownload,"Review before saving",compact=true)},text={UiText("Replace this phone's jobs and shifts with your latest cloud backup? Save a manual backup first if you want to keep both.",fontSize=12.sp)},confirmButton={TextButton(onClick={restoreConfirm=false;task {val uid=repo.auth.currentUser!!.uid;val (next,revision)=repo.restored();check(repo.auth.currentUser?.uid==uid);restoreData(next);repo.bind(uid,revision);queueCloudBackup(context,uid)}}){UiText("Restore backup",fontSize=12.sp)}},dismissButton={TextButton(onClick={restoreConfirm=false}){UiText("Cancel",fontSize=12.sp)}})
    if(enableConfirm) AlertDialog(onDismissRequest={enableConfirm=false},title={ScreenHeading("Cloud backup",Icons.Outlined.CloudUpload,"Automatic Cloud Backup",compact=true)},text={UiText("Upload this phone's data to your account? If another phone has a newer backup, restore it first.",fontSize=12.sp)},confirmButton={TextButton(onClick={enableConfirm=false;task {
        val uid=status.uid!!;if(!repo.bound(uid)) {check(status.revision==0){"Restore existing backup first."};repo.bind(uid,0)}
        repo.reportPreferences(status.monthlyEmail,status.yearlyEmail,data.preferences.language);repo.backup(data);queueCloudBackup(context,uid)
    }}){UiText("Back up now",fontSize=12.sp)}},dismissButton={TextButton(onClick={enableConfirm=false}){UiText("Cancel",fontSize=12.sp)}})
    if(deleteConfirm) AlertDialog(onDismissRequest={deleteConfirm=false},title={ScreenHeading("Delete account",Icons.Outlined.DeleteOutline,"Account & Premium",compact=true)},text={UiText("Permanently delete your account and cloud backups? Local jobs stay on this phone. Cancel your subscription separately in Google Play. You may need to sign in again.",fontSize=12.sp)},confirmButton={TextButton(onClick={deleteConfirm=false;task {stopCloudBackup(context,status.uid);repo.deleteAccount();clearGoogleSession(context)}}){UiText("Delete account",fontSize=12.sp,color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleteConfirm=false}){UiText("Cancel",fontSize=12.sp)}})
}

@Composable private fun GoogleAccountButton(label: String, enabled: Boolean, onClick: ()->Unit) {
    OutlinedButton(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=44.dp),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=androidx.compose.ui.graphics.Color.White,contentColor=androidx.compose.ui.graphics.Color(0xFF1F1F1F)),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)) {
        Image(painterResource(R.drawable.google_signin_logo),null,Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp));UiText(label,fontSize=14.sp,fontWeight=FontWeight.Medium)
    }
}
