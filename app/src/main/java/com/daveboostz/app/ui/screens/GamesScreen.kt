package com.daveboostz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveboostz.app.config.FeatureRegistry
import com.daveboostz.app.config.GameCatalogEntry
import com.daveboostz.app.config.PublishedRemoteConfig
import com.daveboostz.app.data.CrosshairStyle
import com.daveboostz.app.data.PerGameSettingsEntity
import com.daveboostz.app.data.UserPreferences
import com.daveboostz.app.hardware.DeviceTelemetry
import com.daveboostz.app.ui.theme.DaveBoostzPalette

@Composable
fun GamesScreen(
    remoteConfig: PublishedRemoteConfig,
    telemetry: DeviceTelemetry,
    userPrefs: UserPreferences,
    selectedGame: GameCatalogEntry?,
    perGameSettings: PerGameSettingsEntity,
    activeSessionSeconds: Long?,
    onSelectGame: (GameCatalogEntry) -> Unit,
    onSaveGameSettings: (PerGameSettingsEntity) -> Unit,
    onStartSession: (GameCatalogEntry) -> Unit,
    onStopSession: () -> Unit,
    onUpdateCrosshair: (Boolean, CrosshairStyle, Float, Float) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var profileFilter by remember { mutableStateOf("ALL") }

    val filteredGames = remember(remoteConfig.games, searchQuery, profileFilter) {
        remoteConfig.games.filter { game ->
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                game.name.lowercase().contains(q) ||
                game.packageName.lowercase().contains(q)
            val matchesProfile = profileFilter == "ALL" || game.recommendedProfile == profileFilter
            matchesQuery && matchesProfile
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Column: Searchable & Filterable Game Library
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Game Library", color = DaveBoostzPalette.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search games or packages...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL", "LOW-END", "BALANCED", "HIGH-END").forEach { prof ->
                    FilterChip(
                        selected = profileFilter == prof,
                        onClick = { profileFilter = prof },
                        label = { Text(prof, fontSize = 11.sp) }
                    )
                }
            }

            if (filteredGames.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No games match filter.", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredGames, key = { it.id }) { game ->
                        val isSelected = selectedGame?.id == game.id
                        Button(
                            onClick = { onSelectGame(game) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) DaveBoostzPalette.AccentRed else DaveBoostzPalette.SurfaceElevated
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(game.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${game.recommendedProfile} · ${game.compatibility}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Per-Game Profile + Session Tracking + Crosshair Studio
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .background(DaveBoostzPalette.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, DaveBoostzPalette.Border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedGame != null) {
                Text(selectedGame.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(selectedGame.notes, color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activeSessionSeconds == null) {
                        Button(
                            onClick = { onStartSession(selectedGame) },
                            colors = ButtonDefaults.buttonColors(containerColor = DaveBoostzPalette.AccentRed)
                        ) {
                            Text("Start Session Tracker", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onStopSession,
                            colors = ButtonDefaults.buttonColors(containerColor = DaveBoostzPalette.SurfaceElevated)
                        ) {
                            Text("Stop & Save Session (${activeSessionSeconds}s)", color = DaveBoostzPalette.StatusNominal, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = DaveBoostzPalette.Border)

                Text("Per-Game Profile (Room Persisted)", color = DaveBoostzPalette.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-start HUD overlay", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
                    Switch(
                        checked = perGameSettings.overlayEnabled,
                        onCheckedChange = { onSaveGameSettings(perGameSettings.copy(overlayEnabled = it)) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Priority DND during session", color = DaveBoostzPalette.TextMuted, fontSize = 12.sp)
                    Switch(
                        checked = perGameSettings.blockNotifications,
                        onCheckedChange = { onSaveGameSettings(perGameSettings.copy(blockNotifications = it)) }
                    )
                }

                Text("Target Display Refresh Rate (Hardware Supported)", color = DaveBoostzPalette.TextMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    telemetry.supportedRefreshRates.forEach { hz ->
                        FilterChip(
                            selected = perGameSettings.targetFps == hz,
                            onClick = { onSaveGameSettings(perGameSettings.copy(targetFps = hz)) },
                            label = { Text("${hz} Hz", fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (remoteConfig.isFeatureEnabled(FeatureRegistry.CROSSHAIR_STUDIO)) {
                HorizontalDivider(color = DaveBoostzPalette.Border)
                Text("Crosshair Studio", color = DaveBoostzPalette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    remoteConfig.content.crosshairFairPlayWarning,
                    color = DaveBoostzPalette.StatusWarning,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Crosshair in HUD", color = DaveBoostzPalette.TextPrimary, fontSize = 12.sp)
                    Switch(
                        checked = userPrefs.crosshairEnabled,
                        onCheckedChange = {
                            onUpdateCrosshair(it, userPrefs.crosshairStyle, userPrefs.crosshairSizeDp, userPrefs.crosshairOpacity)
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CrosshairStyle.entries.forEach { style ->
                        FilterChip(
                            selected = userPrefs.crosshairStyle == style,
                            onClick = {
                                onUpdateCrosshair(userPrefs.crosshairEnabled, style, userPrefs.crosshairSizeDp, userPrefs.crosshairOpacity)
                            },
                            label = { Text(style.label, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Reticle Size (${userPrefs.crosshairSizeDp.toInt()} dp)", color = DaveBoostzPalette.TextMuted, fontSize = 11.sp)
                Slider(
                    value = userPrefs.crosshairSizeDp,
                    onValueChange = {
                        onUpdateCrosshair(userPrefs.crosshairEnabled, userPrefs.crosshairStyle, it, userPrefs.crosshairOpacity)
                    },
                    valueRange = 12f..48f
                )
            }
        }
    }
}
