package com.paytimeshift.pts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun UpdateOffer(ready:Boolean,update:()->Unit,later:()->Unit,store:Boolean=false) {
    AlertDialog(onDismissRequest=later,containerColor=MaterialTheme.colorScheme.surface,
        title={ScreenHeading(if(ready) "Update ready" else "New version available",Icons.Outlined.SystemUpdate,"PTS · Pay Time Shift",compact=true)},
        text={UiText(if(ready) "The update has downloaded. Restart PTS to finish installing it. Your saved data stays on this phone." else if(store) "A new PTS version is available. Open Google Play to install it." else "A new PTS version is available on Google Play. Download it in the background and keep using the app.",fontSize=12.sp)},
        confirmButton={Button(onClick=update,contentPadding=PaddingValues(horizontal=12.dp,vertical=6.dp)){Icon(Icons.Outlined.SystemUpdate,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));UiText(if(ready) "Restart to update" else if(store) "Open Google Play" else "Update",fontSize=12.sp)}},
        dismissButton={TextButton(onClick=later){UiText("Later",fontSize=12.sp)}})
}
