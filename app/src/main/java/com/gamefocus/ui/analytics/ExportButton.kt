package com.gamefocus.ui.analytics

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gamefocus.utils.ExportManager

@Composable
fun ExportButton(
    context: Context,
    exportManager: ExportManager,
    label: String,
    fileName: String,
    mimeType: String,
    borderColor: Color,
    textColor: Color
) {
    val pendingUri = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { destUri ->
        val tempUri = pendingUri.value
        if (destUri != null && tempUri != null) {
            try {
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    context.contentResolver.openInputStream(tempUri)?.use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
        pendingUri.value = null
    }

    OutlinedButton(
        onClick = {
            exportManager.generateTempFile(fileName, mimeType) { uri ->
                if (uri != null) {
                    pendingUri.value = uri
                    launcher.launch(fileName)
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
    ) {
        Text(text = label)
    }
}
