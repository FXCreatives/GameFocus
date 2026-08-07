package com.gamefocus.ui.permissions

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamefocus.utils.UsagePermissionHelper

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    activity: Activity
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Usage Access Permission Required",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "GameFocus needs permission to detect when you're playing games to track your gaming sessions.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                UsagePermissionHelper.openUsageSettings(activity)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onPermissionGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Permission")
        }
    }
}