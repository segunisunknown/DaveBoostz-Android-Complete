package com.daveboostz.app.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class DaveBoostzNotificationListener : NotificationListenerService() {
    companion object {
        @Volatile
        var blockHeadsUpPackages: Set<String> = emptySet()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg in blockHeadsUpPackages) {
            snoozeNotification(sbn.key, 60_000L * 15L)
        }
    }
}
