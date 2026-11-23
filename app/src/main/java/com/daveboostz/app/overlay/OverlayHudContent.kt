package com.daveboostz.app.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveboostz.app.config.FeatureRegistry
import com.daveboostz.app.config.PublishedRemoteConfig
import com.daveboostz.app.data.CrosshairStyle
import com.daveboostz.app.data.UserPreferences
import com.daveboostz.app.hardware.DeviceTelemetry
import com.daveboostz.app.shizuku.ShizukuMode
import kotlin.math.roundToInt

/** Lightweight in-game HUD: one sculpted command island instead of two full-height side rails. */
@Composable
fun OverlayHudContent(
    remoteConfig: PublishedRemoteConfig,
    userPrefs: UserPreferences,
    telemetry: DeviceTelemetry,
    measuredFps: Int?,
    shizukuMode: ShizukuMode,
    activeGameName: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleCrosshair: () -> Unit,
    onCloseOverlay: () -> Unit
) {
    val cfg = remoteConfig.overlay
    val red = Color(0xFFFF3B45)
    val glass = Color(0xFF101116).copy(alpha = cfg.panelOpacityPercent.coerceIn(58, 96) / 100f)
    var bubbleX by remember { mutableFloatStateOf(if (cfg.defaultBubbleEdge == "left") 18f else 190f) }
    var bubbleY by remember { mutableFloatStateOf(170f) }

    val fpsText = when {
        !remoteConfig.isFeatureEnabled(FeatureRegistry.OVERLAY_FPS_COUNTER) || !cfg.fpsEnabled -> null
        shizukuMode != ShizukuMode.FULL_MODE_ACTIVE -> "FPS · Shizuku needed"
        measuredFps != null -> "$measuredFps FPS"
        else -> "FPS · unavailable"
    }

    Box(Modifier.fillMaxSize()) {
        if (remoteConfig.isFeatureEnabled(FeatureRegistry.CROSSHAIR_STUDIO) && userPrefs.crosshairEnabled) {
            val color = runCatching { Color(android.graphics.Color.parseColor(userPrefs.crosshairColorHex)) }
                .getOrDefault(red).copy(alpha = userPrefs.crosshairOpacity)
            Canvas(Modifier.align(Alignment.Center).size(userPrefs.crosshairSizeDp.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f
                val stroke = 2.dp.toPx()
                when (userPrefs.crosshairStyle) {
                    CrosshairStyle.CLASSIC_CROSS -> {
                        val gap = r * .28f
                        drawLine(color, androidx.compose.ui.geometry.Offset(cx-r,cy), androidx.compose.ui.geometry.Offset(cx-gap,cy), stroke)
                        drawLine(color, androidx.compose.ui.geometry.Offset(cx+gap,cy), androidx.compose.ui.geometry.Offset(cx+r,cy), stroke)
                        drawLine(color, androidx.compose.ui.geometry.Offset(cx,cy-r), androidx.compose.ui.geometry.Offset(cx,cy-gap), stroke)
                        drawLine(color, androidx.compose.ui.geometry.Offset(cx,cy+gap), androidx.compose.ui.geometry.Offset(cx,cy+r), stroke)
                        drawCircle(color, 2.dp.toPx(), androidx.compose.ui.geometry.Offset(cx,cy))
                    }
                    CrosshairStyle.CENTER_DOT -> drawCircle(color, 3.5.dp.toPx())
                    CrosshairStyle.CIRCLE_RETICLE -> {
                        drawCircle(color, r * .72f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                        drawCircle(color, 2.dp.toPx())
                    }
                    CrosshairStyle.OPEN_BRACKET -> {
                        val arm = r * .45f
                        drawLine(color, androidx.compose.ui.geometry.Offset(cx-r,cy-arm), androidx.compose.ui.geometry.Offset(cx-r,cy+arm), stroke)
                        drawLine(color, androidx.compose.ui.geometry.Offset(cx+r,cy-arm), androidx.compose.ui.geometry.Offset(cx+r,cy+arm), stroke)
                        drawCircle(color, 2.5.dp.toPx())
                    }
                }
            }
        }

        if (remoteConfig.isFeatureEnabled(FeatureRegistry.OVERLAY_MINI_HUD) && cfg.miniHudEnabled) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xE6101116))
                    .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fpsText?.let { Text(it, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                if (cfg.cpuTempEnabled) Text("${telemetry.batteryTempC}°C", color = if (telemetry.batteryTempC >= cfg.highTempThresholdC) red else Color.LightGray, fontSize = 12.sp)
                if (cfg.ramOverlayEnabled) Text("RAM ${telemetry.ramPercent}%", color = Color.LightGray, fontSize = 12.sp)
                Text("${telemetry.batteryPercent}%", color = if (telemetry.batteryPercent <= cfg.lowBatteryThresholdPct) Color(0xFFFFC36A) else Color(0xFF8BE0AE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (remoteConfig.isFeatureEnabled(FeatureRegistry.OVERLAY_FLOATING_BUBBLE) && cfg.bubbleEnabled && !isExpanded) {
            Box(
                modifier = Modifier.offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }
                    .size(52.dp).clip(CircleShape)
                    .background(Color(0xF0111218))
                    .border(1.5.dp, red.copy(alpha = .9f), CircleShape)
                    .pointerInput(cfg.positionLocked) {
                        if (!cfg.positionLocked) detectDragGestures { change, drag ->
                            change.consume()
                            bubbleX = (bubbleX + drag.x).coerceAtLeast(0f)
                            bubbleY = (bubbleY + drag.y).coerceAtLeast(0f)
                        }
                    }
                    .clickable { onToggleExpanded() },
                contentAlignment = Alignment.Center
            ) {
                Text("D", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                Canvas(Modifier.matchParentSize()) { drawArc(red, -95f, 275f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())) }
            }
        }

        if (remoteConfig.isFeatureEnabled(FeatureRegistry.OVERLAY_SIDE_PANELS) && cfg.panelsEnabled) {
            // One bottom-centred sculpted island replaces the old full-height side rail.
            // Keep geometry and animation GPU-light: opacity + scale only, no backdrop blur.
            AnimatedVisibility(
                visible = isExpanded,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(start = 14.dp, end = 14.dp, bottom = 24.dp),
                enter = fadeIn() + scaleIn(initialScale = .94f),
                exit = fadeOut() + scaleOut(targetScale = .97f)
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 38.dp, bottomStart = 26.dp, topEnd = 54.dp, bottomEnd = 26.dp))
                        .background(glass)
                        .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(topStart = 38.dp, bottomStart = 26.dp, topEnd = 54.dp, bottomEnd = 26.dp))
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(red))
                        Column(Modifier.weight(1f)) {
                            Text(remoteConfig.branding.appName.uppercase(), color = red, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                            Text(activeGameName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Text("×", color = Color.LightGray, fontSize = 22.sp, modifier = Modifier.clickable { onToggleExpanded() }.padding(horizontal = 4.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricChip("CPU", if (telemetry.cpuFreqMhz > 0) "${telemetry.cpuFreqMhz} MHz" else "N/A", Modifier.weight(1f))
                        MetricChip("TEMP", "${telemetry.batteryTempC}°C", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricChip("MEMORY", "${telemetry.ramPercent}%", Modifier.weight(1f))
                        MetricChip("DISPLAY", if (telemetry.refreshRateHz > 0) "${telemetry.refreshRateHz} Hz" else "N/A", Modifier.weight(1f))
                    }
                    if (fpsText != null) {
                        Text(fpsText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("QUICK CONTROLS", color = Color(0xFF9A9AA5), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    if (remoteConfig.isFeatureEnabled(FeatureRegistry.CROSSHAIR_STUDIO)) {
                        ActionPill(
                            label = if (userPrefs.crosshairEnabled) "Crosshair enabled" else "Enable crosshair",
                            active = userPrefs.crosshairEnabled,
                            onClick = onToggleCrosshair
                        )
                    }
                    ActionPill(label = "Collapse game HUD", active = false, onClick = onToggleExpanded)
                    ActionPill(label = "Stop overlay", active = false, destructive = true, onClick = onCloseOverlay)
                    Text("Live device telemetry · ${if (shizukuMode == ShizukuMode.FULL_MODE_ACTIVE) "Shizuku connected" else "Standard mode"}", color = Color(0xFF8D8D98), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = .055f)).padding(horizontal = 11.dp, vertical = 9.dp)) {
        Text(label, color = Color(0xFF9696A1), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ActionPill(label: String, active: Boolean, destructive: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(when { destructive -> Color(0xFF491B22); active -> Color(0xFF562027); else -> Color.White.copy(alpha = .07f) })
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (active) Color(0xFFFF5962) else Color(0xFF777780)))
        Spacer(Modifier.width(9.dp))
        Text(label, color = if (destructive) Color(0xFFFFA0A5) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
