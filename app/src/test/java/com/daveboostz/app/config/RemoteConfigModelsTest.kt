package com.daveboostz.app.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigModelsTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `overlay validated clamps out-of-range bounds`() {
        val raw = OverlayRemoteConfig(
            miniHudStyle = "invalid_style",
            defaultBubbleEdge = "top",
            panelOpacityPercent = 10,
            hudScale = 3.5f,
            refreshIntervalMs = 50,
            highTempThresholdC = 99,
            lowFpsThreshold = 5,
            lowBatteryThresholdPct = 90
        )

        val clean = raw.validated()
        assertEquals("compact_pill", clean.miniHudStyle)
        assertEquals("right", clean.defaultBubbleEdge)
        assertEquals(50, clean.panelOpacityPercent)
        assertEquals(1.35f, clean.hudScale, 0.001f)
        assertEquals(500, clean.refreshIntervalMs)
        assertEquals(65, clean.highTempThresholdC)
        assertEquals(15, clean.lowFpsThreshold)
        assertEquals(50, clean.lowBatteryThresholdPct)
    }

    @Test
    fun `published config gates non-monitor features under maintenance mode`() {
        val cfg = PublishedRemoteConfig(maintenanceMode = true).validated()
        assertTrue(cfg.isFeatureEnabled(FeatureRegistry.HARDWARE_MONITOR))
        assertFalse(cfg.isFeatureEnabled(FeatureRegistry.OVERLAY_MASTER))
        assertFalse(cfg.isFeatureEnabled(FeatureRegistry.BOOST_BASIC_MODE))
    }

    @Test
    fun `feature registry maintains 24 unique feature identifiers`() {
        assertEquals(24, FeatureRegistry.ALL_FEATURE_IDS.size)
        assertEquals(24, FeatureRegistry.ALL_FEATURE_IDS.toSet().size)
    }

    @Test
    fun `supabase published payload round-trips cleanly through kotlinx serialization`() {
        val original = PublishedRemoteConfig(
            publishedVersionNumber = 7,
            branding = BrandingRemoteConfig(appName = "DaveBoostz", accentColor = "#ef4444"),
            overlay = OverlayRemoteConfig(panelOpacityPercent = 90, hudScale = 1.1f)
        ).validated()

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<PublishedRemoteConfig>(encoded).validated()

        assertEquals(7, decoded.publishedVersionNumber)
        assertEquals("DaveBoostz", decoded.branding.appName)
        assertEquals(90, decoded.overlay.panelOpacityPercent)
        assertEquals(1.1f, decoded.overlay.hudScale, 0.001f)
    }
}
