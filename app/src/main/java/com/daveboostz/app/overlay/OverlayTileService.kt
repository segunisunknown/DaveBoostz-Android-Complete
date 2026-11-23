package com.daveboostz.app.overlay

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class OverlayTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (OverlayForegroundService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (OverlayForegroundService.isRunning) {
            startService(Intent(this, OverlayForegroundService::class.java).setAction(OverlayForegroundService.ACTION_STOP))
        } else if (OverlayPermissionHelper.getStatus(this).canDrawOverlays) {
            startForegroundService(Intent(this, OverlayForegroundService::class.java).setAction(OverlayForegroundService.ACTION_START))
        } else {
            startActivityAndCollapse(OverlayPermissionHelper.buildManageOverlayIntent(this))
        }
    }
}
