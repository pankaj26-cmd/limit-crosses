package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.CombinedUsageReport
import com.limitcrosses.app.domain.repository.UsageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _usageState = MutableStateFlow<CombinedUsageReport?>(null)
    val usageState = _usageState.asStateFlow()

    private val _resetTimeFormatted = MutableStateFlow("")
    val resetTimeFormatted = _resetTimeFormatted.asStateFlow()

    init {
        viewModelScope.launch {
            usageRepository.getCombinedUsageFlow().collect { report ->
                _usageState.value = report
                formatResetTime(report.resetTimeEpochMs)
            }
        }

        // Periodic polling loop (every 30 seconds) to ensure real-time dashboard accuracy
        viewModelScope.launch {
            while (isActive) {
                usageRepository.refreshUsage()
                delay(30_000)
            }
        }
    }

    fun manualRefresh() {
        viewModelScope.launch {
            usageRepository.refreshUsage()
        }
    }

    private fun formatResetTime(epochMs: Long) {
        val diffMs = epochMs - System.currentTimeMillis()
        if (diffMs <= 0) {
            _resetTimeFormatted.value = "12:00 AM (Upcoming)"
            return
        }
        val hours = diffMs / (1000 * 60 * 60)
        val minutes = (diffMs / (1000 * 60)) % 60
        _resetTimeFormatted.value = "in ${hours}h ${minutes}m"
    }
}

