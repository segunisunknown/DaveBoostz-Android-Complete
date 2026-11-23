package com.daveboostz.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsStore by preferencesDataStore(name = "daveboostz_user_prefs")

enum class CrosshairStyle(val label: String) {
    CLASSIC_CROSS("Classic Cross"),
    CENTER_DOT("Precision Dot"),
    CIRCLE_RETICLE("Tactical Ring"),
    OPEN_BRACKET("Open Bracket")
}

data class UserPreferences(
    val crosshairEnabled: Boolean = false,
    val crosshairStyle: CrosshairStyle = CrosshairStyle.CLASSIC_CROSS,
    val crosshairColorHex: String = "#EF4444",
    val crosshairSizeDp: Float = 24f,
    val crosshairOpacity: Float = 0.9f,
    val hudScaleOverride: Float = 1.0f,
    val notificationsEnabled: Boolean = true,
    val telemetryOptIn: Boolean = false,
    val darkAccentTheme: String = "crimson"
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CROSSHAIR_ENABLED = booleanPreferencesKey("crosshair_enabled")
        val CROSSHAIR_STYLE = stringPreferencesKey("crosshair_style")
        val CROSSHAIR_COLOR = stringPreferencesKey("crosshair_color")
        val CROSSHAIR_SIZE = floatPreferencesKey("crosshair_size")
        val CROSSHAIR_OPACITY = floatPreferencesKey("crosshair_opacity")
        val HUD_SCALE = floatPreferencesKey("hud_scale")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TELEMETRY_OPT_IN = booleanPreferencesKey("telemetry_opt_in")
        val THEME_ACCENT = stringPreferencesKey("theme_accent")
    }

    val preferences: Flow<UserPreferences> = context.userPrefsStore.data.map { prefs ->
        val styleName = prefs[Keys.CROSSHAIR_STYLE] ?: CrosshairStyle.CLASSIC_CROSS.name
        val style = runCatching { CrosshairStyle.valueOf(styleName) }
            .getOrDefault(CrosshairStyle.CLASSIC_CROSS)
        UserPreferences(
            crosshairEnabled = prefs[Keys.CROSSHAIR_ENABLED] ?: false,
            crosshairStyle = style,
            crosshairColorHex = prefs[Keys.CROSSHAIR_COLOR] ?: "#EF4444",
            crosshairSizeDp = (prefs[Keys.CROSSHAIR_SIZE] ?: 24f).coerceIn(12f, 48f),
            crosshairOpacity = (prefs[Keys.CROSSHAIR_OPACITY] ?: 0.9f).coerceIn(0.2f, 1.0f),
            hudScaleOverride = (prefs[Keys.HUD_SCALE] ?: 1.0f).coerceIn(0.75f, 1.35f),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            telemetryOptIn = prefs[Keys.TELEMETRY_OPT_IN] ?: false,
            darkAccentTheme = prefs[Keys.THEME_ACCENT] ?: "crimson"
        )
    }

    suspend fun updateCrosshair(
        enabled: Boolean? = null,
        style: CrosshairStyle? = null,
        colorHex: String? = null,
        sizeDp: Float? = null,
        opacity: Float? = null
    ) {
        context.userPrefsStore.edit { prefs ->
            enabled?.let { prefs[Keys.CROSSHAIR_ENABLED] = it }
            style?.let { prefs[Keys.CROSSHAIR_STYLE] = it.name }
            colorHex?.let { prefs[Keys.CROSSHAIR_COLOR] = it }
            sizeDp?.let { prefs[Keys.CROSSHAIR_SIZE] = it.coerceIn(12f, 48f) }
            opacity?.let { prefs[Keys.CROSSHAIR_OPACITY] = it.coerceIn(0.2f, 1.0f) }
        }
    }

    suspend fun updateSettings(
        hudScale: Float? = null,
        notificationsEnabled: Boolean? = null,
        telemetryOptIn: Boolean? = null,
        darkAccentTheme: String? = null
    ) {
        context.userPrefsStore.edit { prefs ->
            hudScale?.let { prefs[Keys.HUD_SCALE] = it.coerceIn(0.75f, 1.35f) }
            notificationsEnabled?.let { prefs[Keys.NOTIFICATIONS_ENABLED] = it }
            telemetryOptIn?.let { prefs[Keys.TELEMETRY_OPT_IN] = it }
            darkAccentTheme?.let { prefs[Keys.THEME_ACCENT] = it }
        }
    }
}
