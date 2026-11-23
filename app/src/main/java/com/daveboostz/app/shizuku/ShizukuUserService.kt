package com.daveboostz.app.shizuku

import android.os.Process
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Bound Shizuku UserService (Shizuku 13.1.5 documented pattern).
 * Only executes strictly allowlisted commands; never runs arbitrary shell strings.
 */
class ShizukuUserService @Keep constructor() : IShizukuUserService.Stub() {

    override fun destroy() {
        Process.killProcess(Process.myPid())
    }

    override fun executeAllowlisted(actionId: String, targetPackage: String): String {
        val safePkg = targetPackage.takeIf { Regex("^[a-zA-Z0-9._]+$").matches(it) }
            ?: return "ERROR:Invalid package identifier"

        val command: List<String> = when (actionId) {
            "FORCE_STOP" -> listOf("am", "force-stop", safePkg)
            "DENY_BG" -> listOf("cmd", "appops", "set", safePkg, "RUN_IN_BACKGROUND", "deny")
            "ALLOW_BG" -> listOf("cmd", "appops", "set", safePkg, "RUN_IN_BACKGROUND", "allow")
            "STANDBY_RARE" -> listOf("am", "set-standby-bucket", safePkg, "rare")
            "GAME_MODE_PERF" -> listOf("cmd", "game", "mode", "performance", safePkg)
            "GAME_MODE_STANDARD" -> listOf("cmd", "game", "mode", "standard", safePkg)
            "COMPILE_SPEED" -> listOf("cmd", "package", "compile", "-m", "speed-profile", "-f", safePkg)
            else -> return "ERROR:Command not in allowlist"
        }

        return runCommand(command)
    }

    override fun readSurfaceFlingerStats(layerHint: String): String {
        return runCommand(listOf("dumpsys", "SurfaceFlinger", "--latency"))
    }

    private fun runCommand(command: List<String>): String {
        return try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) "OK:$output" else "EXIT($exitCode):$output"
        } catch (e: Exception) {
            "ERROR:${e.message}"
        }
    }
}
