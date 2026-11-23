package com.daveboostz.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daveboostz.app.boost.BoostEngine
import com.daveboostz.app.config.FeatureRegistry
import com.daveboostz.app.config.GameCatalogEntry
import com.daveboostz.app.config.RemoteConfigRepository
import com.daveboostz.app.data.DaveBoostzDatabase
import com.daveboostz.app.data.GameSessionEntity
import com.daveboostz.app.data.PerGameSettingsEntity
import com.daveboostz.app.data.UserPreferences
import com.daveboostz.app.data.UserPreferencesRepository
import com.daveboostz.app.hardware.HardwareMonitor
import com.daveboostz.app.overlay.OverlayForegroundService
import com.daveboostz.app.overlay.OverlayPermissionHelper
import com.daveboostz.app.shizuku.ShizukuManager
import com.daveboostz.app.shizuku.ShizukuMode
import com.daveboostz.app.ui.screens.DashboardScreen
import com.daveboostz.app.ui.screens.GamesScreen
import com.daveboostz.app.ui.screens.MonitorScreen
import com.daveboostz.app.ui.screens.SettingsScreen
import com.daveboostz.app.ui.screens.SystemScreen
import com.daveboostz.app.ui.screens.StartupSequence
import com.daveboostz.app.ui.theme.DaveBoostzPalette
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class MainTab(val label: String) {
    DASHBOARD("Dashboard"),
    GAMES("Games"),
    MONITOR("Monitor"),
    SYSTEM("System"),
    SETTINGS("Settings")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var remoteConfigRepository: RemoteConfigRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var database: DaveBoostzDatabase
    @Inject lateinit var hardwareMonitor: HardwareMonitor
    @Inject lateinit var shizukuManager: ShizukuManager
    @Inject lateinit var boostEngine: BoostEngine

    override fun onResume() {
        super.onResume()
        hardwareMonitor.sampleNow()
        shizukuManager.refreshStatus()
        remoteConfigRepository.refreshNow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hardwareMonitor.sampleNow()

        setContent {
            val remoteConfig by remoteConfigRepository.config.collectAsState()
            val syncStatus by remoteConfigRepository.syncStatus.collectAsState()
            val lastSyncedAtMs by remoteConfigRepository.lastSyncedAtMs.collectAsState()
            val userPrefs by userPreferencesRepository.preferences.collectAsState(initial = UserPreferences())
            val telemetry by hardwareMonitor.telemetry.collectAsState()
            val shizukuMode by shizukuManager.mode.collectAsState()
            val sessions by database.dao().observeSessions().collectAsState(initial = emptyList())

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var showStartup by remember { mutableStateOf(true) }

            var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }
            var permissionStatus by remember { mutableStateOf(OverlayPermissionHelper.getStatus(context)) }
            var selectedGame by remember { mutableStateOf<GameCatalogEntry?>(null) }
            var perGameSettings by remember { mutableStateOf(PerGameSettingsEntity("default")) }
            var statusNote by remember { mutableStateOf("") }

            var activeSessionGame by remember { mutableStateOf<GameCatalogEntry?>(null) }
            var sessionStartedAtMs by remember { mutableStateOf<Long?>(null) }
            var sessionElapsedSec by remember { mutableStateOf<Long?>(null) }
            var sessionPeakTempC by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(remoteConfig.games) {
                if (selectedGame == null && remoteConfig.games.isNotEmpty()) {
                    selectedGame = remoteConfig.games.first()
                }
            }

            LaunchedEffect(selectedGame?.packageName) {
                val pkg = selectedGame?.packageName ?: return@LaunchedEffect
                perGameSettings = database.dao().getSettings(pkg) ?: PerGameSettingsEntity(packageName = pkg)
            }

            LaunchedEffect(sessionStartedAtMs) {
                val startMs = sessionStartedAtMs
                if (startMs == null) {
                    sessionElapsedSec = null
                    return@LaunchedEffect
                }
                while (sessionStartedAtMs != null) {
                    sessionElapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).coerceAtLeast(0L)
                    val curTemp = hardwareMonitor.telemetry.value.batteryTempC
                    if (curTemp > sessionPeakTempC) sessionPeakTempC = curTemp
                    delay(1000L)
                }
            }

            if (showStartup) {
                StartupSequence(onFinished = { showStartup = false })
            } else MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = DaveBoostzPalette.AccentRed,
                    background = DaveBoostzPalette.Background,
                    surface = DaveBoostzPalette.Surface
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DaveBoostzPalette.Background
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            containerColor = DaveBoostzPalette.Surface,
                            contentColor = DaveBoostzPalette.TextPrimary
                        ) {
                            Spacer(Modifier.height(8.dp))
                            MainTab.entries.forEach { tab ->
                                val icon = when (tab) {
                                    MainTab.DASHBOARD -> Icons.Default.Dashboard
                                    MainTab.GAMES -> Icons.Default.Gamepad
                                    MainTab.MONITOR -> Icons.Default.Memory
                                    MainTab.SYSTEM -> Icons.Default.Security
                                    MainTab.SETTINGS -> Icons.Default.Settings
                                }
                                NavigationRailItem(
                                    selected = activeTab == tab,
                                    onClick = {
                                        activeTab = tab
                                        permissionStatus = OverlayPermissionHelper.getStatus(context)
                                    },
                                    icon = { Icon(icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontSize = 10.sp) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = DaveBoostzPalette.AccentRed,
                                        selectedTextColor = Color.White,
                                        indicatorColor = DaveBoostzPalette.AccentRed.copy(alpha = 0.16f),
                                        unselectedIconColor = DaveBoostzPalette.TextMuted,
                                        unselectedTextColor = DaveBoostzPalette.TextMuted
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(DaveBoostzPalette.Background)
                        ) {
                            when (activeTab) {
                                MainTab.DASHBOARD -> DashboardScreen(
                                    remoteConfig = remoteConfig,
                                    telemetry = telemetry,
                                    shizukuMode = shizukuMode,
                                    permissionStatus = permissionStatus,
                                    sessions = sessions,
                                    syncStatus = syncStatus,
                                    statusNote = statusNote,
                                    activeSessionSeconds = sessionElapsedSec,
                                    onBoostAndStartOverlay = {
                                        val game = selectedGame ?: remoteConfig.games.firstOrNull()
                                        val res = boostEngine.boostSelectedApps(emptyList(), game?.packageName)
                                        statusNote = "Boosted (${res.modeUsed}) · Freed ${res.freedMb} MB"
                                        val overlayAllowed = remoteConfig.isFeatureEnabled(FeatureRegistry.OVERLAY_MASTER) &&
                                            remoteConfig.overlay.masterEnabled &&
                                            remoteConfig.overlay.autoStartOnGameLaunch
                                        if (overlayAllowed) {
                                            permissionStatus = OverlayPermissionHelper.getStatus(context)
                                            if (permissionStatus.canDrawOverlays) {
                                                val svc = Intent(context, OverlayForegroundService::class.java)
                                                    .setAction(OverlayForegroundService.ACTION_START)
                                                    .putExtra(OverlayForegroundService.EXTRA_GAME_NAME, game?.name ?: "DaveBoostz")
                                                    .putExtra(OverlayForegroundService.EXTRA_GAME_PACKAGE, game?.packageName ?: "")
                                                context.startForegroundService(svc)
                                            } else {
                                                context.startActivity(OverlayPermissionHelper.buildManageOverlayIntent(context))
                                            }
                                        }
                                    },
                                    onToggleOverlayOnly = {
                                        permissionStatus = OverlayPermissionHelper.getStatus(context)
                                        if (!permissionStatus.canDrawOverlays) {
                                            context.startActivity(OverlayPermissionHelper.buildManageOverlayIntent(context))
                                        } else {
                                            val svc = Intent(context, OverlayForegroundService::class.java)
                                                .setAction(OverlayForegroundService.ACTION_START)
                                            context.startForegroundService(svc)
                                        }
                                    },
                                    onOpenGamesTab = { activeTab = MainTab.GAMES },
                                    onOpenSystemTab = { activeTab = MainTab.SYSTEM }
                                )

                                MainTab.GAMES -> GamesScreen(
                                    remoteConfig = remoteConfig,
                                    telemetry = telemetry,
                                    userPrefs = userPrefs,
                                    selectedGame = selectedGame,
                                    perGameSettings = perGameSettings,
                                    activeSessionSeconds = sessionElapsedSec,
                                    onSelectGame = { selectedGame = it },
                                    onSaveGameSettings = { updated ->
                                        perGameSettings = updated
                                        scope.launch { database.dao().saveSettings(updated) }
                                    },
                                    onStartSession = { game ->
                                        activeSessionGame = game
                                        sessionPeakTempC = telemetry.batteryTempC
                                        sessionStartedAtMs = System.currentTimeMillis()
                                    },
                                    onStopSession = {
                                        val game = activeSessionGame
                                        val startMs = sessionStartedAtMs
                                        val dur = sessionElapsedSec ?: 0L
                                        val peakTemp = sessionPeakTempC
                                        sessionStartedAtMs = null
                                        activeSessionGame = null
                                        if (game != null && startMs != null) {
                                            val avgFps = shizukuManager.readRealFpsOrNull()
                                            scope.launch {
                                                database.dao().insertSession(
                                                    GameSessionEntity(
                                                        packageName = game.packageName,
                                                        gameName = game.name,
                                                        startedAtMs = startMs,
                                                        durationSec = dur,
                                                        avgFps = avgFps,
                                                        maxTempC = peakTemp,
                                                        profileUsed = game.recommendedProfile
                                                    )
                                                )
                                                if (userPrefs.telemetryOptIn) {
                                                    remoteConfigRepository.reportOptInSession(
                                                        gamePackage = game.packageName,
                                                        deviceModel = telemetry.deviceModel,
                                                        androidVersion = telemetry.androidVersion,
                                                        durationSec = dur,
                                                        avgFps = avgFps,
                                                        maxTempC = peakTemp,
                                                        shizukuActive = shizukuMode == ShizukuMode.FULL_MODE_ACTIVE
                                                    )
                                                }
                                            }
                                            statusNote = "Saved ${dur}s session for ${game.name}"
                                        }
                                    },
                                    onUpdateCrosshair = { enabled, style, sizeDp, opacity ->
                                        scope.launch {
                                            userPreferencesRepository.updateCrosshair(
                                                enabled = enabled,
                                                style = style,
                                                sizeDp = sizeDp,
                                                opacity = opacity
                                            )
                                        }
                                    }
                                )

                                MainTab.MONITOR -> MonitorScreen(
                                    telemetry = telemetry,
                                    sessions = sessions,
                                    onSampleNow = { hardwareMonitor.sampleNow() },
                                    onProbeLatency = { scope.launch { hardwareMonitor.probeNetworkLatencyMs() } }
                                )

                                MainTab.SYSTEM -> SystemScreen(
                                    remoteConfig = remoteConfig,
                                    shizukuMode = shizukuMode,
                                    permissionStatus = permissionStatus,
                                    onCheckShizuku = {
                                        if (shizukuMode == ShizukuMode.PERMISSION_MISSING) {
                                            shizukuManager.requestPermission()
                                        } else {
                                            shizukuManager.refreshStatus()
                                        }
                                    },
                                    onRequestOverlayPermission = {
                                        context.startActivity(OverlayPermissionHelper.buildManageOverlayIntent(context))
                                    },
                                    onRequestDndPermission = {
                                        context.startActivity(OverlayPermissionHelper.buildNotificationPolicyIntent())
                                    },
                                    onRequestNotificationSettings = {
                                        context.startActivity(OverlayPermissionHelper.buildAppNotificationSettingsIntent(context))
                                    },
                                    onRequestUsageAccess = {
                                        context.startActivity(OverlayPermissionHelper.buildUsageAccessIntent())
                                    }
                                )

                                MainTab.SETTINGS -> SettingsScreen(
                                    remoteConfig = remoteConfig,
                                    userPrefs = userPrefs,
                                    telemetry = telemetry,
                                    syncStatus = syncStatus,
                                    lastSyncedAtMs = lastSyncedAtMs,
                                    onUpdateHudScale = { scale ->
                                        scope.launch { userPreferencesRepository.updateSettings(hudScale = scale) }
                                    },
                                    onToggleNotifications = { enabled ->
                                        scope.launch { userPreferencesRepository.updateSettings(notificationsEnabled = enabled) }
                                    },
                                    onToggleTelemetryOptIn = { optIn ->
                                        scope.launch { userPreferencesRepository.updateSettings(telemetryOptIn = optIn) }
                                    },
                                    onClearSessions = {
                                        scope.launch { database.dao().deleteAllSessions() }
                                        statusNote = "Cleared local Room session history"
                                    },
                                    onRefreshRemoteConfig = { remoteConfigRepository.refreshNow() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
