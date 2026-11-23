package com.daveboostz.app.network

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class DaveBoostzVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val blockedPackages = intent?.getStringArrayListExtra("blocked_packages").orEmpty()
        val builder = Builder()
            .setSession("DaveBoostz Gaming Firewall")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")

        // Only route user-selected blocked background apps through the local firewall; never touch games
        for (pkg in blockedPackages) {
            runCatching { builder.addAllowedApplication(pkg) }
        }

        vpnInterface?.close()
        vpnInterface = runCatching { builder.establish() }.getOrNull()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        super.onDestroy()
    }
}
