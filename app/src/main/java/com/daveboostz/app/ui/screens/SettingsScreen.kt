package com.daveboostz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveboostz.app.BuildConfig
import com.daveboostz.app.config.PublishedRemoteConfig
import com.daveboostz.app.data.UserPreferences
import com.daveboostz.app.hardware.DeviceTelemetry
import com.daveboostz.app.ui.theme.DaveBoostzPalette
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(
    remoteConfig: PublishedRemoteConfig,
    userPrefs: UserPreferences,
    telemetry: DeviceTelemetry,
    syncStatus: String,
    lastSyncedAtMs: Long,
    onUpdateHudScale: (Float) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleTelemetryOptIn: (Boolean) -> Unit,
    onClearSessions: () -> Unit,
    onRefreshRemoteConfig: () -> Unit
) {
    val lastSyncFormatted = if (lastSyncedAtMs > 0L) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSyncedAtMs))
    } else {
        "Never synced remotely"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings & Diagnostics", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)

        // HUD & Notification Preferences
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("HUD & Notification Preferences", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("HUD Scale Override (${String.format("%.2f", userPrefs.hudScaleOverride)}x)", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
            Slider(
                value = userPrefs.hudScaleOverride,
                onValueChange = onUpdateHudScale,
                valueRange = 0.75f..1.35f
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Overlay status alerts", color = DaveBoostzPalette.TextPrimary, fontSize = 12.sp)
                Switch(
                    checked = userPrefs.notificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
            }
        }

        // Privacy Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Privacy & Local Data Controls", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Share anonymous session telemetry", color = DaveBoostzPalette.TextPrimary, fontSize = 12.sp)
                    Text("Off by default. Only sends app version, model, duration, max temp, and avg FPS.", color = DaveBoostzPalette.TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = userPrefs.telemetryOptIn,
                    onCheckedChange = onToggleTelemetryOptIn
                )
            }
            OutlinedButton(onClick = onClearSessions) {
                Text("Clear Local Room Session History", color = DaveBoostzPalette.StatusError, fontSize = 12.sp)
            }
        }

        // Remote Config Sync & App Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("App Info & Remote Sync", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onRefreshRemoteConfig,
                    colors = ButtonDefaults.buttonColors(containerColor = DaveBoostzPalette.AccentRed)
                ) {
                    Text("Sync Config", color = Color.White, fontSize = 12.sp)
                }
            }
            Text("Installed Version: v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
            Text("Latest Admin Release: v${remoteConfig.release.latestVersion} (Min v${remoteConfig.release.minSupportedVersion})", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
            Text("Sync Status: $syncStatus · Last sync: $lastSyncFormatted", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
            Text("Diagnostics: ${telemetry.deviceModel} · API ${telemetry.apiLevel} · ${telemetry.architecture}", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
        }
    }
}
