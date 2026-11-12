package com.limitcrosses.app.ui.viewmodel

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.limitcrosses.app.core.model.PermissionState
import com.limitcrosses.app.platform.usage.UsageStatsHelper
import com.limitcrosses.app.platform.vpn.LimitCrossesVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionsViewModel(
    private val context: Context,
    private val usageStatsHelper: UsageStatsHelper
) : ViewModel() {

    private val _permState = MutableStateFlow(PermissionState())
    val permState = _permState.asStateFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val hasUsage = usageStatsHelper.hasUsagePermission()
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.areNotificationsEnabled() ?: false
        } else {
            true
        }
        val hasVpn = LimitCrossesVpnService.isPrepared(context)

        _permState.value = PermissionState(
            hasUsageStats = hasUsage,
            hasNotifications = hasNotifications,
            hasVpn = hasVpn
        )
    }

    fun canProceedToLimit(): Boolean {
        // Usage permission is mandatory for accurate tracking
        return _permState.value.hasUsageStats
    }
}

