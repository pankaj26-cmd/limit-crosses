package com.limitcrosses.app.platform.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Optional Device Admin / Device Owner receiver.
 * Kept completely isolated from consumer self-discipline mode.
 * Activated strictly via explicit enterprise / adb provisioning:
 * dpm set-device-owner com.limitcrosses.app/.platform.receiver.LimitCrossesDeviceAdminReceiver
 */
class LimitCrossesDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("DeviceAdminReceiver", "Device Admin enabled for Limit Crosses.")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i("DeviceAdminReceiver", "Device Admin disabled for Limit Crosses.")
    }
}

