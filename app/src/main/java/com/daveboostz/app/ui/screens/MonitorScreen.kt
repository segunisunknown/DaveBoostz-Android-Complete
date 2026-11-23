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
import com.daveboostz.app.data.GameSessionEntity
import com.daveboostz.app.hardware.DeviceTelemetry
import com.daveboostz.app.ui.theme.DaveBoostzPalette

@Composable
fun MonitorScreen(
    telemetry: DeviceTelemetry,
    sessions: List<GameSessionEntity>,
    onSampleNow: () -> Unit,
    onProbeLatency: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Device Telemetry & Diagnostics", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Genuine OS-exposed sensors only · ${telemetry.deviceModel} (Android ${telemetry.androidVersion})", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onProbeLatency) {
                    Text("Probe Latency", color = Color.White, fontSize = 12.sp)
                }
                Button(
                    onClick = onSampleNow,
                    colors = ButtonDefaults.buttonColors(containerColor = DaveBoostzPalette.AccentRed)
                ) {
                    Text("Refresh Sample", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU Core Frequencies
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("CPU Clocks (${telemetry.cpuCores} Cores)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (telemetry.cpuCoreFreqsMhz.all { it == 0 }) {
                    Text("Per-core sysfs cpufreq access is restricted by SELinux on this device.", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
                } else {
                    telemetry.cpuCoreFreqsMhz.forEachIndexed { idx, mhz ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Core $idx", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
                            Text(if (mhz > 0) "$mhz MHz" else "Offline", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // RAM, Battery, Network, Display
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Memory, Power & Display", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                MetricLine("RAM Used", "${telemetry.ramUsedMb} / ${telemetry.ramTotalMb} MB (${telemetry.ramPercent}%)")
                MetricLine("Battery Level", "${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "On Battery"})")
                MetricLine("Battery Temp / Volt", "${telemetry.batteryTempC}°C · ${telemetry.batteryVoltageMv} mV")
                MetricLine("Active Refresh Rate", "${telemetry.refreshRateHz} Hz (Supported: ${telemetry.supportedRefreshRates.joinToString { "${it}Hz" }})")
                MetricLine("Network & TCP Probe", "${telemetry.networkType} · ${telemetry.networkLatencyMs?.let { "${it} ms" } ?: "Tap Probe Latency"}")
            }
        }

        // Recorded Gaming Session History (Room)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Local Session History (Room DB)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (sessions.isEmpty()) {
                Text("No completed sessions recorded yet. Start a session tracker in the Games tab.", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
            } else {
                sessions.take(8).forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${s.gameName} (${s.profileUsed})", color = Color.White, fontSize = 12.sp)
                        Text(
                            "${s.durationSec}s · Max ${s.maxTempC}°C · ${s.avgFps?.let { "$it FPS" } ?: "FPS N/A"}",
                            color = DaveBoostzPalette.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
