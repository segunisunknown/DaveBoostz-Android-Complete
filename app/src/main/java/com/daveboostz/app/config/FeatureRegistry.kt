package com.daveboostz.app.config

/**
 * Authoritative registry of every user-facing feature in the DaveBoostz Android app.
 * Keys match the Supabase PostgreSQL `daveboostz_config` (`id = 'published'`) `payload_json.featureFlags` map 1:1.
 */
object FeatureRegistry {
    const val GAME_SPACE_WHEEL = "game_space_wheel"
    const val INSTALLED_APPS_PICKER = "installed_apps_picker"
    const val GAME_X_AI_PROFILES = "game_x_ai"
    const val BOOST_BASIC_MODE = "boost_basic_mode"
    const val BOOST_FULL_SHIZUKU = "shizuku_engine"
    const val PER_GAME_SETTINGS = "per_game_settings"
    const val SPEED_MODE_FPS = "speed_mode_fps"
    const val OVERLAY_MASTER = "overlay_hud"
    const val OVERLAY_FLOATING_BUBBLE = "overlay_bubble"
    const val OVERLAY_SIDE_PANELS = "overlay_panels"
    const val OVERLAY_MINI_HUD = "overlay_mini_hud"
    const val OVERLAY_FPS_COUNTER = "overlay_fps_counter"
    const val OVERLAY_CPU_RAM_MONITOR = "overlay_cpu_ram_monitor"
    const val CROSSHAIR_STUDIO = "crosshair_studio"
    const val SCREEN_RECORDER = "screen_recorder"
    const val SCREENSHOT_STUDIO = "screenshot_studio"
    const val RECORDING_VISIBILITY = "recording_visibility"
    const val VOICE_EFFECTS = "voice_effects"
    const val NETWORK_VPN_FIREWALL = "network_vpn_firewall"
    const val NETWORK_DIAGNOSTICS = "network_diagnostics"
    const val BATTERY_THERMAL_MONITOR = "battery_thermal_monitor"
    const val HARDWARE_MONITOR = "hardware_monitor"
    const val SESSION_ANALYTICS_ROOM = "session_analytics"
    const val GAMING_NOTIFICATIONS_DND = "gaming_notifications_dnd"

    val ALL_FEATURE_IDS = listOf(
        GAME_SPACE_WHEEL,
        INSTALLED_APPS_PICKER,
        GAME_X_AI_PROFILES,
        BOOST_BASIC_MODE,
        BOOST_FULL_SHIZUKU,
        PER_GAME_SETTINGS,
        SPEED_MODE_FPS,
        OVERLAY_MASTER,
        OVERLAY_FLOATING_BUBBLE,
        OVERLAY_SIDE_PANELS,
        OVERLAY_MINI_HUD,
        OVERLAY_FPS_COUNTER,
        OVERLAY_CPU_RAM_MONITOR,
        CROSSHAIR_STUDIO,
        SCREEN_RECORDER,
        SCREENSHOT_STUDIO,
        RECORDING_VISIBILITY,
        VOICE_EFFECTS,
        NETWORK_VPN_FIREWALL,
        NETWORK_DIAGNOSTICS,
        BATTERY_THERMAL_MONITOR,
        HARDWARE_MONITOR,
        SESSION_ANALYTICS_ROOM,
        GAMING_NOTIFICATIONS_DND
    )
}
