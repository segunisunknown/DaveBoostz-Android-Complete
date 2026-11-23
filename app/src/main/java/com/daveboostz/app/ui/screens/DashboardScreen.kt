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
import com.daveboostz.app.config.FeatureRegistry
import com.daveboostz.app.config.PublishedRemoteConfig
import com.daveboostz.app.data.GameSessionEntity
import com.daveboostz.app.hardware.DeviceTelemetry
import com.daveboostz.app.overlay.OverlayPermissionStatus
import com.daveboostz.app.shizuku.ShizukuMode
import com.daveboostz.app.ui.theme.DaveBoostzPalette

@Composable
fun DashboardScreen(
    remoteConfig: PublishedRemoteConfig,
    telemetry: DeviceTelemetry,
    shizukuMode: ShizukuMode,
    permissionStatus: OverlayPermissionStatus,
    sessions: List<GameSessionEntity>,
    syncStatus: String,
    statusNote: String,
    activeSessionSeconds: Long?,
    onBoostAndStartOverlay: () -> Unit,
    onToggleOverlayOnly: () -> Unit,
    onOpenGamesTab: () -> Unit,
    onOpenSystemTab: () -> Unit
) {
    val activeFeaturesCount = remoteConfig.featureFlags.values.count { it }
    val latestSession = sessions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        remoteConfig.branding.appName.uppercase(),
                        color = DaveBoostzPalette.AccentRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        remoteConfig.branding.tagline,
                        color = DaveBoostzPalette.TextMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = if (shizukuMode == ShizukuMode.FULL_MODE_ACTIVE) "Full Mode (Shizuku)" else "Basic Mode",
                    color = if (shizukuMode == ShizukuMode.FULL_MODE_ACTIVE) DaveBoostzPalette.StatusNominal else DaveBoostzPalette.StatusWarning,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(syncStatus, color = DaveBoostzPalette.TextMuted, fontSize = 11.sp)

            if (shizukuMode != ShizukuMode.FULL_MODE_ACTIVE && remoteConfig.branding.shizukuNoticeEnabled) {
                Text(
                    remoteConfig.content.basicModeBanner,
                    color = DaveBoostzPalette.StatusWarning,
                    fontSize = 12.sp
                )
            }

            if (statusNote.isNotBlank()) {
                Text(statusNote, color = DaveBoostzPalette.StatusNominal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onBoostAndStartOverlay,
                    colors = ButtonDefaults.buttonColors(containerColor = DaveBoostzPalette.AccentRed)
                ) {
                    Text(remoteConfig.content.startBoostButton, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onToggleOverlayOnly) {
                    Text(
                        if (permissionStatus.canDrawOverlays) "Launch HUD Overlay" else "Grant Overlay Access",
                        color = DaveBoostzPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Real Hardware Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryStatCard(
                modifier = Modifier.weight(1f),
                label = "CPU Peak",
                value = if (telemetry.cpuFreqMhz > 0) "${telemetry.cpuFreqMhz} MHz" else "Restricted",
                detail = "${telemetry.cpuCores} cores · ${telemetry.architecture}"
            )
            TelemetryStatCard(
                modifier = Modifier.weight(1f),
                label = "Memory (RAM)",
                value = "${telemetry.ramPercent}%",
                detail = "${telemetry.ramUsedMb}/${telemetry.ramTotalMb} MB"
            )
            TelemetryStatCard(
                modifier = Modifier.weight(1f),
                label = "Battery & Thermal",
                value = "${telemetry.batteryPercent}%",
                detail = "${telemetry.batteryTempC}°C · ${if (telemetry.isCharging) "Charging" else "Discharging"}"
            )
            TelemetryStatCard(
                modifier = Modifier.weight(1f),
                label = "Display & Net",
                value = "${telemetry.refreshRateHz} Hz",
                detail = "${telemetry.networkType}${telemetry.networkLatencyMs?.let { " · ${it}ms" } ?: ""}"
            )
        }

        // Game Library & Session Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Game Library Summary", color = DaveBoostzPalette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${remoteConfig.games.size} catalog profiles · $activeFeaturesCount/${FeatureRegistry.ALL_FEATURE_IDS.size} features enabled",
                    color = DaveBoostzPalette.TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    remoteConfig.content.defaultTip,
                    color = DaveBoostzPalette.TextMuted,
                    fontSize = 12.sp
                )
                TextButton(onClick = onOpenGamesTab, contentPadding = PaddingValues(0.dp)) {
                    Text("Open Games & Crosshair Studio", color = DaveBoostzPalette.AccentRed, fontSize = 12.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Gaming Session Status", color = DaveBoostzPalette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (activeSessionSeconds != null) {
                    Text(
                        "Recording live session: ${activeSessionSeconds}s",
                        color = DaveBoostzPalette.StatusNominal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (latestSession != null) {
                    Text(
                        "Last: ${latestSession.gameName} (${latestSession.durationSec}s · Peak ${latestSession.maxTempC}°C)",
                        color = DaveBoostzPalette.TextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        "No sessions logged in Room yet.",
                        color = DaveBoostzPalette.TextMuted,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onOpenSystemTab, contentPadding = PaddingValues(0.dp)) {
                    Text("Check Permissions & Shizuku", color = DaveBoostzPalette.AccentRed, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    detail: String
) {
    Column(
        modifier = modifier
            .background(DaveBoostzPalette.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = DaveBoostzPalette.TextMuted, fontSize = 11.sp)
        Text(value, color = DaveBoostzPalette.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = DaveBoostzPalette.TextMuted, fontSize = 11.sp, maxLines = 1)
    }
}
