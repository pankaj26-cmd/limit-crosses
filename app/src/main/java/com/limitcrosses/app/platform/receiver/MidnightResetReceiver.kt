package com.limitcrosses.app.platform.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.domain.repository.UsageRepository
import com.limitcrosses.app.platform.service.AppBlockerOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class MidnightResetReceiver : BroadcastReceiver(), KoinComponent {

    private val usageRepository: UsageRepository by inject()
    private val localDataStorage: LocalDataStorage by inject()
    private val backendClient: BackendClient by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("MidnightResetReceiver", "Local midnight reset or timezone event received: ${intent?.action}")

        // Re-arm for the next night
        scheduleNextMidnightAlarm(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Immediately hide any active blocker overlay on screen
                AppBlockerOverlayManager.hideOverlay()

                // 2. Refresh usage repository to force zeroed baseline for new calendar day
                usageRepository.refreshUsage()

                // 3. If logged in as client user with invite binding, publish 0s telemetry to cloud relay
                val currentUserId = localDataStorage.getCurrentUserId()
                if (currentUserId != null) {
                    val user = localDataStorage.getUserById(currentUserId)
                    if (user != null && user.role != UserRole.ADMIN && !user.registeredViaInviteCode.isNullOrBlank()) {
                        backendClient.publishUsageTelemetry(currentUserId)
                    }
                }
            } catch (e: Exception) {
                Log.e("MidnightResetReceiver", "Error executing midnight reset sequence: ${e.message}")
            }
        }
    }

    companion object {
        const val ACTION_MIDNIGHT_RESET = "com.limitcrosses.app.ACTION_MIDNIGHT_RESET"
        private const val ALARM_REQUEST_CODE = 9021

        /**
         * Schedules exact RTC_WAKEUP alarm for the next upcoming 12:00:00 AM midnight.
         */
        fun scheduleNextMidnightAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val triggerEpochMs = calendar.timeInMillis

                val intent = Intent(context, MidnightResetReceiver::class.java).apply {
                    action = ACTION_MIDNIGHT_RESET
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                }
                Log.d("MidnightResetReceiver", "Next exact midnight alarm scheduled for: $triggerEpochMs")
            } catch (e: Exception) {
                Log.e("MidnightResetReceiver", "Failed to schedule exact midnight alarm: ${e.message}")
            }
        }
    }
}
