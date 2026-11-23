package com.daveboostz.app.boost

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.daveboostz.app.config.FeatureRegistry
import com.daveboostz.app.config.RemoteConfigRepository
import com.daveboostz.app.shizuku.ShizukuManager
import com.daveboostz.app.shizuku.ShizukuMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class BoostResult(
    val ramBeforeMb: Long,
    val ramAfterMb: Long,
    val freedMb: Long,
    val cleanedCount: Int,
    val modeUsed: String,
    val dndApplied: Boolean,
    val notes: String
)

@Singleton
class BoostEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuManager: ShizukuManager,
    private val remoteConfigRepository: RemoteConfigRepository
) {
    private var previousInterruptionFilter: Int? = null
    private val protectedPrefixes = listOf(
        "com.android.systemui",
        "com.android.phone",
        "com.android.providers",
        "com.google.android.inputmethod",
        "com.samsung.android.honeyboard"
    )

    fun boostSelectedApps(selectedPackages: List<String>, targetGamePackage: String?): BoostResult {
        val cfg = remoteConfigRepository.config.value
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val beforeAvail = readAvailMb(am)
        var cleaned = 0

        val safeTargets = selectedPackages.filter { pkg ->
            pkg != context.packageName &&
                pkg != targetGamePackage &&
                protectedPrefixes.none { pkg.startsWith(it) }
        }

        val useShizuku = cfg.isFeatureEnabled(FeatureRegistry.BOOST_FULL_SHIZUKU) &&
            shizukuManager.mode.value == ShizukuMode.FULL_MODE_ACTIVE

        if (cfg.isFeatureEnabled(FeatureRegistry.BOOST_BASIC_MODE) || useShizuku) {
            for (pkg in safeTargets) {
                if (useShizuku) {
                    shizukuManager.executeAllowlisted("FORCE_STOP", pkg)
                    shizukuManager.executeAllowlisted("DENY_BG", pkg)
                    shizukuManager.executeAllowlisted("STANDBY_RARE", pkg)
                } else {
                    @Suppress("DEPRECATION")
                    am.killBackgroundProcesses(pkg)
                }
                cleaned++
            }
            if (useShizuku && !targetGamePackage.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                shizukuManager.executeAllowlisted("GAME_MODE_PERF", targetGamePackage)
            }
        }

        val dndApplied = applyGameDndIfEnabled(cfg.isFeatureEnabled(FeatureRegistry.GAMING_NOTIFICATIONS_DND))
        val afterAvail = readAvailMb(am)
        val freed = (afterAvail - beforeAvail).coerceAtLeast(0L)

        return BoostResult(
            ramBeforeMb = beforeAvail,
            ramAfterMb = afterAvail,
            freedMb = freed,
            cleanedCount = cleaned,
            modeUsed = if (useShizuku) "Full (Shizuku)" else "Basic",
            dndApplied = dndApplied,
            notes = if (useShizuku) "Allowlisted Shizuku tuning applied" else "Standard killBackgroundProcesses applied"
        )
    }

    fun restoreAll(selectedPackages: List<String>, targetGamePackage: String?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prev = previousInterruptionFilter
        if (prev != null && nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(prev)
            previousInterruptionFilter = null
        }

        if (shizukuManager.mode.value == ShizukuMode.FULL_MODE_ACTIVE) {
            for (pkg in selectedPackages) {
                shizukuManager.executeAllowlisted("ALLOW_BG", pkg)
            }
            if (!targetGamePackage.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                shizukuManager.executeAllowlisted("GAME_MODE_STANDARD", targetGamePackage)
            }
        }
    }

    private fun applyGameDndIfEnabled(featureEnabled: Boolean): Boolean {
        if (!featureEnabled) return false
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return false
        if (previousInterruptionFilter == null) {
            previousInterruptionFilter = nm.currentInterruptionFilter
        }
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        return true
    }

    private fun readAvailMb(am: ActivityManager): Long {
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem / (1024L * 1024L)
    }
}
