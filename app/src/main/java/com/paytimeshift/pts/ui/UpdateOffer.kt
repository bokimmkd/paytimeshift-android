package com.paytimeshift.pts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paytimeshift.pts.domain.UpdateStage
import com.paytimeshift.pts.domain.UpdateTransfer

@Composable internal fun UpdateOffer(ready:Boolean,update:()->Unit,later:()->Unit,store:Boolean=false,failed:Boolean=false) {
    AlertDialog(onDismissRequest=later,containerColor=MaterialTheme.colorScheme.surface,
        title={ScreenHeading(if(ready) "Update ready" else "New version available",Icons.Outlined.SystemUpdate,"PTS · Pay Time Shift",compact=true)},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {UiText(if(ready) "The update has downloaded. Restart PTS to finish installing it. Your saved data stays on this phone." else if(store) "A new PTS version is available. Open Google Play to install it." else "A new PTS version is available on Google Play. Download it in the background and keep using the app.",fontSize=12.sp)
            if(failed) UiText("Could not finish installing. Tap Restart to try again.",fontSize=12.sp,color=MaterialTheme.colorScheme.error)}},
        confirmButton={Button(onClick=update,contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)){Icon(Icons.Outlined.SystemUpdate,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText(if(ready) "Restart to update" else if(store) "Open Google Play" else "Update",fontSize=12.sp)}},
        dismissButton={TextButton(onClick=later){UiText("Later",fontSize=12.sp)}})
}

/** Compact, non-modal status; all normal app actions remain available during download. */
@Composable internal fun UpdateProgress(transfer:UpdateTransfer) {
    if(transfer.stage !in listOf(UpdateStage.Waiting,UpdateStage.Downloading,UpdateStage.Installing)) return
    Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.SystemUpdate,null,Modifier.size(18.dp))
                UiText(when(transfer.stage) {UpdateStage.Waiting->"Waiting to download…";UpdateStage.Downloading->"Downloading update…";else->"Installing update…"},Modifier.weight(1f),fontSize=12.sp)
                if(transfer.stage==UpdateStage.Downloading) transfer.fraction?.let {fraction->
                    Text(java.text.NumberFormat.getPercentInstance(uiLocale()).format(fraction.toDouble()),fontSize=12.sp)
                }
            }
            val fraction=transfer.fraction
            if(transfer.stage==UpdateStage.Downloading && fraction!=null) LinearProgressIndicator(progress={fraction},modifier=Modifier.fillMaxWidth())
            else LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            UiText(if(transfer.stage==UpdateStage.Installing) "Google Play will restart PTS to finish the update." else "You can keep using PTS while the update downloads.",fontSize=10.sp)
        }
    }
}
