package com.daveboostz.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.daveboostz.app.MainActivity
import com.daveboostz.app.config.FeatureRegistry
import com.daveboostz.app.config.RemoteConfigRepository
import com.daveboostz.app.data.UserPreferences
import com.daveboostz.app.data.UserPreferencesRepository
import com.daveboostz.app.hardware.HardwareMonitor
import com.daveboostz.app.shizuku.ShizukuManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns ONE WindowManager overlay window hosting the draggable bubble,
 * full-screen landscape Game Space panels (center touch pass-through), Mini HUD, and Crosshair.
 */
@AndroidEntryPoint
class OverlayForegroundService : Service() {

    @Inject lateinit var remoteConfigRepository: RemoteConfigRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var hardwareMonitor: HardwareMonitor
    @Inject lateinit var shizukuManager: ShizukuManager

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var samplingJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isExpanded = MutableStateFlow(false)
    private val _measuredFps = MutableStateFlow<Int?>(null)
    private val _activeGameName = MutableStateFlow("DaveBoostz")
    private val _activeGamePackage = MutableStateFlow("")
    private var isStopping = false

    companion object {
        const val ACTION_START = "com.daveboostz.app.overlay.START"
        const val ACTION_STOP = "com.daveboostz.app.overlay.STOP"
        const val EXTRA_GAME_NAME = "extra_game_name"
        const val EXTRA_GAME_PACKAGE = "extra_game_package"
        private const val CHANNEL_ID = "daveboostz_overlay_channel"
        private const val NOTIFICATION_ID = 4001

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCleanly()
            return START_NOT_STICKY
        }

        if (!OverlayPermissionHelper.getStatus(this).canDrawOverlays) {
            stopSelf()
            return START_NOT_STICKY
        }

        intent?.getStringExtra(EXTRA_GAME_NAME)?.let { _activeGameName.value = it }
        intent?.getStringExtra(EXTRA_GAME_PACKAGE)?.let { _activeGamePackage.value = it }

        startForegroundCompat()
        isRunning = true
        ensureOverlayAttached()
        startTelemetryLoop()
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureOverlayAttached() {
        if (overlayView != null) return

        val owner = OverlayLifecycleOwner().apply { onCreate() }
        lifecycleOwner = owner

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val remoteConfig by remoteConfigRepository.config.collectAsState()
                val userPrefs by userPreferencesRepository.preferences.collectAsState(initial = UserPreferences())
                val telemetry by hardwareMonitor.telemetry.collectAsState()
                val fps by _measuredFps.collectAsState()
                val expanded by _isExpanded.collectAsState()
                val gameName by _activeGameName.collectAsState()
                val shizukuMode by shizukuManager.mode.collectAsState()

                val masterAllowed = remoteConfig.isFeatureEnabled(FeatureRegistry.OVERLAY_MASTER) &&
                    remoteConfig.overlay.masterEnabled

                if (masterAllowed) {
                    OverlayHudContent(
                        remoteConfig = remoteConfig,
                        userPrefs = userPrefs,
                        telemetry = telemetry,
                        measuredFps = fps,
                        shizukuMode = shizukuMode,
                        activeGameName = gameName,
                        isExpanded = expanded,
                        onToggleExpanded = { _isExpanded.value = !expanded },
                        onToggleCrosshair = {
                            serviceScope.launch {
                                userPreferencesRepository.updateCrosshair(enabled = !userPrefs.crosshairEnabled)
                            }
                        },
                        onCloseOverlay = { stopCleanly() }
                    )
                }
            }
        }

        runCatching {
            windowManager.addView(composeView, layoutParams)
            overlayView = composeView
        }.onFailure {
            owner.onDestroy()
            lifecycleOwner = null
        }
    }

    private fun startTelemetryLoop() {
        samplingJob?.cancel()
        samplingJob = serviceScope.launch {
            while (isActive && !isStopping) {
                val cfg = remoteConfigRepository.config.value
                if (!cfg.isFeatureEnabled(FeatureRegistry.OVERLAY_MASTER) || !cfg.overlay.masterEnabled) {
                    detachOverlayViewOnly()
                } else if (overlayView == null && OverlayPermissionHelper.getStatus(this@OverlayForegroundService).canDrawOverlays) {
                    ensureOverlayAttached()
                }

                hardwareMonitor.sampleNow()
                _measuredFps.value = if (cfg.isFeatureEnabled(FeatureRegistry.OVERLAY_FPS_COUNTER) && cfg.overlay.fpsEnabled) {
                    shizukuManager.readRealFpsOrNull()
                } else {
                    null
                }
                delay(cfg.overlay.refreshIntervalMs.coerceIn(500, 5000).toLong())
            }
        }
    }

    private fun detachOverlayViewOnly() {
        overlayView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        overlayView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    private fun stopCleanly() {
        isStopping = true
        isRunning = false
        samplingJob?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        detachOverlayViewOnly()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isStopping = true
        isRunning = false
        samplingJob?.cancel()
        detachOverlayViewOnly()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DaveBoostz Gaming Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DaveBoostz Gaming HUD Active")
            .setContentText("Monitoring ${_activeGameName.value}")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_view, "Open", openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }
}
