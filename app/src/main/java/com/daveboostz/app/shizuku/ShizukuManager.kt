package com.daveboostz.app.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.daveboostz.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

enum class ShizukuMode {
    NOT_INSTALLED_OR_STOPPED,
    PERMISSION_MISSING,
    FULL_MODE_ACTIVE
}

@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _mode = MutableStateFlow(ShizukuMode.NOT_INSTALLED_OR_STOPPED)
    val mode: StateFlow<ShizukuMode> = _mode.asStateFlow()

    private var userService: IShizukuUserService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShizukuUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_worker")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userService = IShizukuUserService.Stub.asInterface(service)
            refreshStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            refreshStatus()
        }
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            bindServiceIfReady()
        }
        refreshStatus()
    }

    init {
        Shizuku.addRequestPermissionResultListener(permissionListener)
        refreshStatus()
    }

    fun refreshStatus() {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive) {
            _mode.value = ShizukuMode.NOT_INSTALLED_OR_STOPPED
            return
        }
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        if (!granted) {
            _mode.value = ShizukuMode.PERMISSION_MISSING
            return
        }

        bindServiceIfReady()
        _mode.value = ShizukuMode.FULL_MODE_ACTIVE
    }

    fun requestPermission() {
        runCatching {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1001)
            }
        }
    }

    private fun bindServiceIfReady() {
        if (userService == null) {
            runCatching {
                Shizuku.bindUserService(userServiceArgs, serviceConnection)
            }
        }
    }

    fun executeAllowlisted(actionId: String, targetPackage: String): String {
        val service = userService ?: return "ERROR:Shizuku UserService not bound"
        return runCatching { service.executeAllowlisted(actionId, targetPackage) }
            .getOrElse { "ERROR:${it.message}" }
    }

    fun readRealFpsOrNull(): Int? {
        if (_mode.value != ShizukuMode.FULL_MODE_ACTIVE) return null
        val service = userService ?: return null
        val raw = runCatching { service.readSurfaceFlingerStats("") }.getOrNull() ?: return null
        if (!raw.startsWith("OK:")) return null
        val lines = raw.removePrefix("OK:").lines().drop(1).filter { it.isNotBlank() }
        if (lines.size < 3) return null
        val timestamps = lines.mapNotNull { line ->
            line.trim().split(Regex("\\s+")).getOrNull(1)?.toLongOrNull()
        }.filter { it > 0L }
        if (timestamps.size < 2) return null
        val spanNs = timestamps.last() - timestamps.first()
        if (spanNs <= 0L) return null
        val fps = ((timestamps.size - 1) * 1_000_000_000.0 / spanNs).toInt()
        return fps.coerceIn(1, 240)
    }
}
