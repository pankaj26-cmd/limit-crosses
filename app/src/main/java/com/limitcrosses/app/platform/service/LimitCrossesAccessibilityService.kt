package com.limitcrosses.app.platform.service

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.limitcrosses.app.MainActivity
import com.limitcrosses.app.domain.repository.UsageRepository
import com.limitcrosses.app.platform.receiver.LimitCrossesDeviceAdminReceiver
import com.limitcrosses.app.platform.usage.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Accessibility Service for real-time, zero-latency detection of social app window launches.
 * When the user attempts to launch Facebook, Instagram, or Snapchat while their
 * daily allowance is exhausted (0 min remaining):
 * 1. Calls performGlobalAction(GLOBAL_ACTION_HOME) to instantly minimize the social app.
 * 2. Displays the impassable WindowManager overlay via AppBlockerOverlayManager.
 * 3. Brings Limit Crosses (LimitReachedScreen) to the foreground with 5 cognitive puzzle challenges.
 */
class LimitCrossesAccessibilityService : AccessibilityService() {

    companion object {
        fun isAccessibilityEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedComponentName = ComponentName(context, LimitCrossesAccessibilityService::class.java).flattenToString()
            return enabledServices.contains(expectedComponentName)
        }

        fun getAccessibilitySettingsIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    private val usageRepository: UsageRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var lastInterceptionTimeMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        if (UsageStatsHelper.MONITORED_PACKAGES.containsKey(packageName)) {
            serviceScope.launch {
                val report = usageRepository.refreshUsage()
                if (report.isLimitReached) {
                    val now = System.currentTimeMillis()
                    if (now - lastInterceptionTimeMs < 1500L) {
                        return@launch // Debounce rapid window state pulses
                    }
                    lastInterceptionTimeMs = now

                    val appMeta = UsageStatsHelper.MONITORED_PACKAGES[packageName]
                    val appName = appMeta?.first ?: packageName

                    withContext(Dispatchers.Main) {
                        // 1. Instantly minimize the blocked app back to home screen
                        performGlobalAction(GLOBAL_ACTION_HOME)

                        // 2. Display the impassable blocker overlay window
                        AppBlockerOverlayManager.showOverlay(this@LimitCrossesAccessibilityService, packageName, appName)

                        // 3. Launch LimitReachedScreen in Limit Crosses
                        val launchIntent = Intent(this@LimitCrossesAccessibilityService, MainActivity::class.java).apply {
                            action = AppMonitorBlockerService.ACTION_LIMIT_REACHED
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("blocked_pkg", packageName)
                            putExtra("blocked_name", appName)
                        }
                        startActivity(launchIntent)

                        // 4. If Device Owner mode is active, suspend package at OS level
                        try {
                            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                            val adminComponent = ComponentName(this@LimitCrossesAccessibilityService, LimitCrossesDeviceAdminReceiver::class.java)
                            if (dpm != null && dpm.isDeviceOwnerApp(packageName)) {
                                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
