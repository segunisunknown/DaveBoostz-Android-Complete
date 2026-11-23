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
import com.daveboostz.app.config.PublishedRemoteConfig
import com.daveboostz.app.overlay.OverlayPermissionStatus
import com.daveboostz.app.shizuku.ShizukuMode
import com.daveboostz.app.ui.theme.DaveBoostzPalette

@Composable
fun SystemScreen(
    remoteConfig: PublishedRemoteConfig,
    shizukuMode: ShizukuMode,
    permissionStatus: OverlayPermissionStatus,
    onCheckShizuku: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestDndPermission: () -> Unit,
    onRequestNotificationSettings: () -> Unit,
    onRequestUsageAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("System Integration & Permissions", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            "All system operations use documented Android APIs or strictly allowlisted Shizuku actions. Arbitrary shell execution is disabled.",
            color = DaveBoostzPalette.TextMuted,
            fontSize = 12.sp
        )

        // Shizuku Integration Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shizuku 13.1.5 UserService", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when (shizukuMode) {
                            ShizukuMode.FULL_MODE_ACTIVE -> "Connected · SurfaceFlinger FPS sampling & allowlisted app control active"
                            ShizukuMode.PERMISSION_MISSING -> "Shizuku service detected · Tap Authorize to grant permission"
                            ShizukuMode.NOT_INSTALLED_OR_STOPPED -> "Not running · Operating safely in Basic Mode"
                        },
                        color = if (shizukuMode == ShizukuMode.FULL_MODE_ACTIVE) DaveBoostzPalette.StatusNominal else DaveBoostzPalette.StatusWarning,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onCheckShizuku,
                    colors = ButtonDefaults.buttonColors(containerColor = DaveBoostzPalette.AccentRed)
                ) {
                    Text(
                        if (shizukuMode == ShizukuMode.PERMISSION_MISSING) "Authorize Shizuku" else "Refresh Status",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            Text(remoteConfig.branding.shizukuNoticeText, color = DaveBoostzPalette.TextMuted, fontSize = 11.sp)
        }

        // Permission Rows with clear explanations
        PermissionRow(
            title = "Display over other apps (SYSTEM_ALERT_WINDOW)",
            rationale = remoteConfig.content.overlayPermissionRationale,
            granted = permissionStatus.canDrawOverlays,
            actionLabel = "Open Overlay Settings",
            onAction = onRequestOverlayPermission
        )
        PermissionRow(
            title = "Do Not Disturb Policy Access",
            rationale = "Restores your previous interruption mode after gaming sessions finish.",
            granted = permissionStatus.dndPolicyGranted,
            actionLabel = "Grant DND Access",
            onAction = onRequestDndPermission
        )
        PermissionRow(
            title = "Foreground HUD Notifications (POST_NOTIFICATIONS)",
            rationale = "Displays the persistent overlay control notification with quick Stop action.",
            granted = permissionStatus.postNotificationsGranted,
            actionLabel = "Notification Settings",
            onAction = onRequestNotificationSettings
        )
        PermissionRow(
            title = "Usage Stats Access",
            rationale = "Optional: detects active foreground game packages without polling private data.",
            granted = permissionStatus.usageStatsGranted,
            actionLabel = "Grant Usage Access",
            onAction = onRequestUsageAccess
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    rationale: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DaveBoostzPalette.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(rationale, color = DaveBoostzPalette.TextMuted, fontSize = 11.sp)
            Text(
                if (granted) "Granted" else "Not Granted",
                color = if (granted) DaveBoostzPalette.StatusNominal else DaveBoostzPalette.StatusWarning,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = onAction) {
            Text(actionLabel, color = Color.White, fontSize = 12.sp)
        }
    }
}
