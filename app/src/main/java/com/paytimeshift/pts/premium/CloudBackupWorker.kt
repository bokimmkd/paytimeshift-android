package com.paytimeshift.pts.premium

import android.content.Context
import androidx.work.*
import com.paytimeshift.pts.data.LocalStore
import java.util.concurrent.TimeUnit

class CloudBackupWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val repo=PremiumRepository(applicationContext);val uid=inputData.getString("uid") ?: return Result.success()
        if(repo.auth.currentUser?.uid!=uid || !repo.bound(uid)) return Result.success()
        return try {repo.backup(LocalStore(applicationContext).load());Result.success()}
        catch(e: com.google.firebase.functions.FirebaseFunctionsException) {
            if(e.code in listOf(com.google.firebase.functions.FirebaseFunctionsException.Code.ABORTED,com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED,com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION)) Result.failure()
            else if(runAttemptCount<5) Result.retry() else Result.failure()
        } catch(_: Exception) {if(runAttemptCount<5) Result.retry() else Result.failure()}
    }
}
fun queueCloudBackup(context: Context,uid: String?) {
    if(uid==null) return
    val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    val args=workDataOf("uid" to uid);val manager=WorkManager.getInstance(context)
    manager.enqueueUniqueWork("pts-backup-$uid",ExistingWorkPolicy.KEEP,OneTimeWorkRequestBuilder<CloudBackupWorker>().setInputData(args).setConstraints(constraints).setInitialDelay(10,TimeUnit.SECONDS).build())
    manager.enqueueUniquePeriodicWork("pts-backup-periodic-$uid",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<CloudBackupWorker>(15,TimeUnit.MINUTES).setInputData(args).setConstraints(constraints).build())
}
fun stopCloudBackup(context: Context,uid: String?) {if(uid!=null) {WorkManager.getInstance(context).cancelUniqueWork("pts-backup-$uid");WorkManager.getInstance(context).cancelUniqueWork("pts-backup-periodic-$uid")}}
