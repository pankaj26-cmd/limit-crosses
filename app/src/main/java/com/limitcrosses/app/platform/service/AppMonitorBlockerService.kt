package com.limitcrosses.app.platform.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.limitcrosses.app.MainActivity
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.domain.repository.UsageRepository
import com.limitcrosses.app.platform.receiver.LimitCrossesDeviceAdminReceiver
import com.limitcrosses.app.platform.usage.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Background Foreground Service that continuously tracks foreground social app usage
 * (Facebook, Instagram, Snapchat). When the combined daily allowance hits 0,
 * it immediately intercepts the user by launching the LimitReachedScreen
 * over the monitored app and posting a high-priority heads-up warning.
 */
class AppMonitorBlockerService : Service() {

    companion object {
        const val ACTION_START = "com.limitcrosses.app.ACTION_START_MONITOR"
        const val ACTION_STOP = "com.limitcrosses.app.ACTION_STOP_MONITOR"
        const val ACTION_LIMIT_REACHED = "com.limitcrosses.app.ACTION_LIMIT_REACHED"

        private const val CHANNEL_ID = "limit_crosses_guard_channel"
        private const val ALERT_CHANNEL_ID = "limit_crosses_alert_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 9001
        private const val ALERT_NOTIFICATION_ID = 9002

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorBlockerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorBlockerService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val usageStatsHelper: UsageStatsHelper by inject()
    private val usageRepository: UsageRepository by inject()
    private val backendClient: BackendClient by inject()
    private val localDataStorage: LocalDataStorage by inject()

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null
    private var lastInterceptionEpochMs = 0L
    private var lastBackgroundTelemetryPushEpochMs = 0L
    private var lastDayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
        startMonitoringLoop()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Check for midnight day rollover (crossing 12:00:00 AM)
                    val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                    if (currentDay != lastDayOfYear) {
                        lastDayOfYear = currentDay
                        try {
                            AppBlockerOverlayManager.hideOverlay()
                            usageRepository.refreshUsage()
                            val currentUid = localDataStorage.getCurrentUserId()
                            if (currentUid != null) {
                                val user = localDataStorage.getUserById(currentUid)
                                if (user != null && user.role != UserRole.ADMIN && !user.registeredViaInviteCode.isNullOrBlank()) {
                                    backendClient.publishUsageTelemetry(currentUid)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    val foregroundPkg = usageStatsHelper.getForegroundPackage()
                    val isSocialApp = foregroundPkg != null && UsageStatsHelper.MONITORED_PACKAGES.containsKey(foregroundPkg)

                    if (isSocialApp) {
                        val report = usageRepository.refreshUsage()
                        val appMeta = UsageStatsHelper.MONITORED_PACKAGES[foregroundPkg]
                        val appName = appMeta?.first ?: foregroundPkg!!

                        if (report.isLimitReached) {
                            // Enforce rate-limited interception (at most once every 3 seconds to prevent thrashing)
                            val now = System.currentTimeMillis()
                            if (now - lastInterceptionEpochMs > 3000L) {
                                lastInterceptionEpochMs = now
                                interceptSocialApp(foregroundPkg!!, appName)
                            }
                        }
                    }

                    // Background Telemetry Heartbeat (15s if actively using social apps, 45s normal heartbeat)
                    val now = System.currentTimeMillis()
                    val pushInterval = if (isSocialApp) 15_000L else 45_000L
                    if (now - lastBackgroundTelemetryPushEpochMs >= pushInterval) {
                        lastBackgroundTelemetryPushEpochMs = now
                        val currentUid = localDataStorage.getCurrentUserId()
                        if (currentUid != null) {
                            val user = localDataStorage.getUserById(currentUid)
                            if (user != null && user.role != UserRole.ADMIN && !user.registeredViaInviteCode.isNullOrBlank()) {
                                try {
                                    backendClient.publishUsageTelemetry(currentUid)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } catch (_: Exception) {}

                // Poll every 1.5 seconds for responsive foreground interception
                delay(1500L)
            }
        }
    }

    private fun interceptSocialApp(packageName: String, appName: String) {
        // 1. Immediately drop out of the social app back to the launcher home screen
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {}

        // 2. Display the impassable full-screen overlay over the social app
        AppBlockerOverlayManager.showOverlay(applicationContext, packageName, appName)

        // 3. Launch LimitReachedScreen in Limit Crosses
        try {
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                action = ACTION_LIMIT_REACHED
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("blocked_pkg", packageName)
                putExtra("blocked_name", appName)
            }
            startActivity(launchIntent)
        } catch (_: Exception) {}

        // 4. High-priority Heads-up Notification
        postLimitReachedAlert(appName)

        // 5. Optional Enterprise Device Owner Hardware Lock
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = ComponentName(this, LimitCrossesDeviceAdminReceiver::class.java)
            if (dpm != null && dpm.isDeviceOwnerApp(this.packageName)) {
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
            }
        } catch (_: Exception) {}
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Limit Crosses Protection Active")
            .setContentText("Monitoring Facebook, Instagram & Snapchat combined daily limit.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun postLimitReachedAlert(appName: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_LIMIT_REACHED
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Daily Limit Reached for $appName")
            .setContentText("Your 0-45m daily allowance is exhausted. Tap to solve puzzles & request extra time.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val guardChannel = NotificationChannel(
                CHANNEL_ID,
                "Protection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time foreground monitoring status of social apps."
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Limit Reached Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when daily limit is exhausted and an app is intercepted."
            }

            notificationManager.createNotificationChannel(guardChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
    }
}

