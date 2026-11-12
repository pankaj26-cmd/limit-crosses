package com.limitcrosses.app.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.limitcrosses.app.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MidnightResetReceiver : BroadcastReceiver(), KoinComponent {

    private val usageRepository: UsageRepository by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("MidnightResetReceiver", "Local midnight or timezone change broadcast received. Refreshing usage state.")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                usageRepository.refreshUsage()
            } catch (e: Exception) {
                Log.e("MidnightResetReceiver", "Error refreshing usage on midnight reset: ${e.message}")
            }
        }
    }
}

