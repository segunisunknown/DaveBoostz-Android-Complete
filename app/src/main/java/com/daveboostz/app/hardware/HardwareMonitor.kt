package com.daveboostz.app.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceTelemetry(
    val cpuFreqMhz: Int = 0,
    val cpuCoreFreqsMhz: List<Int> = emptyList(),
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val ramPercent: Int = 0,
    val batteryPercent: Int = 100,
    val batteryTempC: Float = 0f,
    val batteryVoltageMv: Int = 0,
    val isCharging: Boolean = false,
    val refreshRateHz: Int = 60,
    val supportedRefreshRates: List<Int> = listOf(60),
    val networkType: String = "Offline",
    val networkConnected: Boolean = false,
    val networkLatencyMs: Int? = null,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
    val architecture: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
    val androidVersion: String = Build.VERSION.RELEASE,
    val apiLevel: Int = Build.VERSION.SDK_INT
)

@Singleton
class HardwareMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _telemetry = MutableStateFlow(DeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    fun sampleNow(): DeviceTelemetry {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024L * 1024L)
        val availMb = memInfo.availMem / (1024L * 1024L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        val ramPct = if (totalMb > 0) ((usedMb * 100) / totalMb).toInt().coerceIn(0, 100) else 0

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempTenths / 10f
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val coreFreqsMhz = readPerCoreCpuFreqsMhz()
        val maxFreqMhz = coreFreqsMhz.maxOrNull() ?: 0

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { context.display }.getOrNull() ?: @Suppress("DEPRECATION") wm.defaultDisplay
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        }
        val activeHz = display?.mode?.refreshRate?.toInt() ?: 60
        val supportedHz = display?.supportedModes?.map { it.refreshRate.toInt() }?.distinct()?.sorted() ?: listOf(60)

        val (netType, netConnected) = readNetworkState()

        val sampled = DeviceTelemetry(
            cpuFreqMhz = maxFreqMhz,
            cpuCoreFreqsMhz = coreFreqsMhz,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            ramUsedMb = usedMb,
            ramTotalMb = totalMb,
            ramPercent = ramPct,
            batteryPercent = batteryPct,
            batteryTempC = tempC,
            batteryVoltageMv = voltageMv,
            isCharging = charging,
            refreshRateHz = activeHz,
            supportedRefreshRates = supportedHz,
            networkType = netType,
            networkConnected = netConnected,
            networkLatencyMs = _telemetry.value.networkLatencyMs
        )
        _telemetry.value = sampled
        return sampled
    }

    suspend fun probeNetworkLatencyMs(): Int? = withContext(Dispatchers.IO) {
        val measured = runCatching {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 443), 1500)
            }
            ((System.nanoTime() - start) / 1_000_000L).toInt().coerceAtLeast(1)
        }.getOrNull()

        _telemetry.value = _telemetry.value.copy(networkLatencyMs = measured)
        measured
    }

    private fun readPerCoreCpuFreqsMhz(): List<Int> {
        val cores = Runtime.getRuntime().availableProcessors()
        return (0 until cores).map { i ->
            val file = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            if (file.canRead()) {
                runCatching { file.readText().trim().toInt() / 1000 }.getOrDefault(0)
            } else {
                0
            }
        }
    }

    private fun readNetworkState(): Pair<String, Boolean> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Offline" to false
        val caps = cm.getNetworkCapabilities(network) ?: return "Offline" to false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Connected"
        }
        return type to hasInternet
    }
}
