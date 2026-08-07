package com.gamefocus.ui.games

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.app.usage.StorageStatsManager
import android.os.Build
import android.os.storage.StorageManager
import android.os.UserHandle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamefocus.data.models.GameApp
import com.gamefocus.ui.theme.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(game: GameApp, onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    var versionName by remember { mutableStateOf("Unknown") }
    var versionCode by remember { mutableStateOf("Unknown") }
    var apkSize by remember { mutableStateOf("Unknown") }
    var userDataSize by remember { mutableStateOf("Unknown") }
    var cacheSize by remember { mutableStateOf("Unknown") }
    var totalStorageUsed by remember { mutableStateOf("Unknown") }

    LaunchedEffect(game.packageName) {
        try {
            val pkgInfo = pm.getPackageInfo(game.packageName, 0)
            versionName = pkgInfo.versionName ?: "Unknown"
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toString()
            }

            // Load app icon from PackageManager
            val appIcon = try {
                pm.getApplicationIcon(game.packageName)
            } catch (_: Exception) {
                null
            }
            // We can't update game.icon here since it's a parameter,
            // but we can store it in a local state for GameIconLarge
            // For now, we'll pass it through - GameIconLarge will use pm directly
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager = context.getSystemService(StorageStatsManager::class.java)
                val storageManager = context.getSystemService(StorageManager::class.java)

                val uid = pkgInfo.applicationInfo?.uid ?: 0
                val userHandle = UserHandle.getUserHandleForUid(uid)

                val apkFile = File(pkgInfo.applicationInfo?.sourceDir ?: "")
                val storageVolume = storageManager?.getStorageVolume(apkFile)
                val uuidString = storageVolume?.uuid
                val uuid = if (uuidString != null) {
                    java.util.UUID.fromString(uuidString)
                } else {
                    StorageManager.UUID_DEFAULT
                }

                val stats = storageStatsManager?.queryStatsForPackage(uuid, game.packageName, userHandle)

                if (stats != null) {
                    apkSize = formatFileSize(stats.appBytes)
                    userDataSize = formatFileSize(stats.dataBytes)
                    cacheSize = formatFileSize(stats.cacheBytes)
                    totalStorageUsed = formatFileSize(stats.appBytes + stats.dataBytes + stats.cacheBytes)
                } else {
                    throw Exception("StorageStats unavailable")
                }
            } else {
                val apkFile = pkgInfo.applicationInfo?.sourceDir?.let { File(it) }
                val apkBytes = apkFile?.length() ?: 0L
                apkSize = formatFileSize(apkBytes)

                val dataDir = pkgInfo.applicationInfo?.dataDir?.let { File(it) }
                val dataBytes = if (dataDir != null && dataDir.exists()) {
                    withContext(Dispatchers.IO) { getDirectorySize(dataDir) }
                } else {
                    0L
                }
                userDataSize = formatFileSize(dataBytes)
                cacheSize = "Unavailable"
                totalStorageUsed = formatFileSize(apkBytes + dataBytes)
            }
        } catch (_: Exception) {
            apkSize = "Unavailable"
            userDataSize = "Unavailable"
            cacheSize = "Unavailable"
            totalStorageUsed = "Unavailable"
        }
    }

    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    
    LaunchedEffect(game.packageName) {
        try {
            appIcon = pm.getApplicationIcon(game.packageName)
        } catch (_: Exception) {
            appIcon = null
        }
    }

    val isInstalled = remember {
        try {
            pm.getApplicationIcon(game.packageName)
            true
        } catch (_: Exception) {
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GamingBackground)
    ) {
        TopAppBar(
            title = { 
                Text(
                    text = if (game.appName.contains("/") || game.appName.contains("{packageName}")) "Game Details" else game.appName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GamingBackground,
                titleContentColor = TextPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GameIconLarge(game)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GamingCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = game.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = game.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GamingCard)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoRow("Version", "$versionName ($versionCode)")
                    InfoRow("Package", game.packageName)
                    InfoRow("APK Size", apkSize)
                    InfoRow("User Data", userDataSize)
                    InfoRow("Cache", cacheSize)
                    InfoRow("Total Storage Used", totalStorageUsed)

                    if (game.installDate > 0) {
                        InfoRow("Install Date", formatDate(game.installDate))
                    }
                    if (game.updateDate > 0) {
                        InfoRow("Last Update", formatDate(game.updateDate))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Action: Open Game
            Button(
                onClick = {
                    val launchIntent = pm.getLaunchIntentForPackage(game.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = isInstalled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    disabledContainerColor = Divider
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isInstalled) "Open Game" else "Game not installed",
                    color = if (isInstalled) GamingBackground else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val playStoreIntent = try {
                            pm.getLaunchIntentForPackage("com.android.vending")
                        } catch (_: Exception) {
                            null
                        }
                        val intent = if (playStoreIntent != null) {
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${game.packageName}"))
                        } else {
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${game.packageName}"))
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Store", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "GameFocus: ${game.appName}\nPackage: ${game.packageName}\nVersion: $versionName")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Game"))
                    },
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameIconLarge(game: GameApp) {
    val pm = androidx.compose.ui.platform.LocalContext.current.packageManager
    val appIcon = remember(game.packageName) {
        try {
            pm.getApplicationIcon(game.packageName)
        } catch (_: Exception) {
            null
        }
    }
    
    Card(
        modifier = Modifier.size(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GamingCard)
    ) {
        if (appIcon != null) {
            Image(
                painter = rememberDrawablePainter(appIcon),
                contentDescription = game.appName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = game.appName,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format("%.1f %s", value, units[digitGroups])
}

private fun getDirectorySize(directory: java.io.File): Long {
    var size = 0L
    if (directory.exists() && directory.isDirectory) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
    }
    return size
}
