package com.daveboostz.app.config

import kotlinx.serialization.Serializable

@Serializable
data class OverlayRemoteConfig(
    val masterEnabled: Boolean = true,
    val bubbleEnabled: Boolean = true,
    val panelsEnabled: Boolean = true,
    val miniHudEnabled: Boolean = true,
    val cpuGaugeEnabled: Boolean = true,
    val gpuGaugeEnabled: Boolean = true,
    val fpsEnabled: Boolean = true,
    val cpuFreqEnabled: Boolean = true,
    val cpuTempEnabled: Boolean = true,
    val ramOverlayEnabled: Boolean = true,
    val touchPassThrough: Boolean = true,
    val edgeSnapEnabled: Boolean = true,
    val autoStartOnGameLaunch: Boolean = true,
    val hideControlsFromRecording: Boolean = true,
    val subOverlayVisibleInRecording: Boolean = true,
    val crosshairVisibleInRecording: Boolean = true,
    val miniHudStyle: String = "compact_pill",
    val defaultBubbleEdge: String = "right",
    val panelOpacityPercent: Int = 92,
    val hudScale: Float = 1.0f,
    val positionLocked: Boolean = false,
    val refreshIntervalMs: Int = 1000,
    val highTempThresholdC: Int = 44,
    val lowFpsThreshold: Int = 40,
    val lowBatteryThresholdPct: Int = 15
) {
    fun validated(): OverlayRemoteConfig {
        val validStyles = setOf("compact_pill", "vertical_stack", "horizontal_bar", "mini_graph")
        return copy(
            miniHudStyle = if (miniHudStyle in validStyles) miniHudStyle else "compact_pill",
            defaultBubbleEdge = if (defaultBubbleEdge == "left") "left" else "right",
            panelOpacityPercent = panelOpacityPercent.coerceIn(50, 100),
            hudScale = hudScale.coerceIn(0.75f, 1.35f),
            refreshIntervalMs = refreshIntervalMs.coerceIn(500, 5000),
            highTempThresholdC = highTempThresholdC.coerceIn(35, 65),
            lowFpsThreshold = lowFpsThreshold.coerceIn(15, 120),
            lowBatteryThresholdPct = lowBatteryThresholdPct.coerceIn(5, 50)
        )
    }
}

@Serializable
data class BrandingRemoteConfig(
    val appName: String = "DaveBoostz",
    val tagline: String = "Precision Android Gaming Control",
    val launchTagline: String = "Engineered for Stable Frame Pacing",
    val accentColor: String = "#ef4444",
    val glowIntensity: String = "balanced",
    val launcherIconAlias: String = "DefaultRed",
    val shizukuDownloadUrl: String = "https://shizuku.rikka.app/download/",
    val shizukuNoticeText: String = "Connect Shizuku for surface latency FPS sampling and allowlisted force-stop actions.",
    val shizukuNoticeEnabled: Boolean = true
)

@Serializable
data class ContentRemoteConfig(
    val onboardingTitle: String = "Welcome to DaveBoostz",
    val onboardingDesc: String = "Monitor real device telemetry, manage background app behavior safely, and configure your floating gaming HUD.",
    val basicModeBanner: String = "Basic mode is active because Shizuku was not detected. Hardware monitoring, background cleanup, and DND still work; connect Shizuku for SurfaceFlinger FPS and deeper tuning.",
    val lockedCardMessage: String = "Requires Shizuku authorization.",
    val startBoostButton: String = "Boost and Launch",
    val comingSoonText: String = "Scheduled for next release",
    val defaultTip: String = "Close unused background apps before starting long gaming sessions.",
    val overlayPermissionRationale: String = "DaveBoostz needs Display over other apps permission to draw the in-game floating HUD and crosshair.",
    val crosshairFairPlayWarning: String = "Fair-Play Notice: Check game terms of service before enabling visual crosshair overlays.",
    val maintenanceMessage: String = "Cloud configuration is undergoing scheduled maintenance. Local profiles continue operating offline."
)

@Serializable
data class GameCatalogEntry(
    val id: String = "",
    val packageName: String = "",
    val name: String = "",
    val iconUrl: String = "",
    val backgroundUrl: String = "",
    val recommendedProfile: String = "BALANCED",
    val compatibility: String = "WORKS",
    val notes: String = ""
)

@Serializable
data class ReleaseRemoteConfig(
    val minSupportedVersion: String = "1.0.0",
    val latestVersion: String = "2.0.0",
    val changelog: String = "",
    val apkUrl: String = "",
    val apkSha256: String = "",
    val forceUpdate: Boolean = false
)

@Serializable
data class PublishedRemoteConfig(
    val publishedVersionNumber: Int = 4,
    val lastSyncedIso: String = "",
    val maintenanceMode: Boolean = false,
    val branding: BrandingRemoteConfig = BrandingRemoteConfig(),
    val content: ContentRemoteConfig = ContentRemoteConfig(),
    val overlay: OverlayRemoteConfig = OverlayRemoteConfig(),
    val release: ReleaseRemoteConfig = ReleaseRemoteConfig(),
    val featureFlags: Map<String, Boolean> = FeatureRegistry.ALL_FEATURE_IDS.associateWith { true },
    val games: List<GameCatalogEntry> = emptyList()
) {
    fun validated(): PublishedRemoteConfig = copy(
        overlay = overlay.validated(),
        branding = branding.copy(
            appName = branding.appName.ifBlank { "DaveBoostz" },
            accentColor = if (Regex("^#[0-9a-fA-F]{6}$").matches(branding.accentColor)) branding.accentColor else "#ef4444"
        )
    )

    fun isFeatureEnabled(featureId: String): Boolean {
        if (maintenanceMode && featureId != FeatureRegistry.HARDWARE_MONITOR) return false
        return featureFlags[featureId] ?: true
    }
}
