package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.CombinedUsageReport
import com.limitcrosses.app.domain.repository.DailyHistoryItem
import com.limitcrosses.app.domain.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class UsageDiagnostics(
    val lastPolledString: String,
    val timezoneString: String,
    val isTrackingActive: Boolean
)

class UsageViewModel(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _currentReport = MutableStateFlow<CombinedUsageReport?>(null)
    val currentReport = _currentReport.asStateFlow()

    private val _historyItems = MutableStateFlow<List<DailyHistoryItem>>(emptyList())
    val historyItems = _historyItems.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        UsageDiagnostics(
            lastPolledString = "Never",
            timezoneString = TimeZone.getDefault().id,
            isTrackingActive = false
        )
    )
    val diagnostics = _diagnostics.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val report = usageRepository.refreshUsage()
            _currentReport.value = report
            _historyItems.value = usageRepository.getDailyHistory()

            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            _diagnostics.value = UsageDiagnostics(
                lastPolledString = sdf.format(Date(report.lastPolledEpochMs)),
                timezoneString = "${TimeZone.getDefault().displayName} (${TimeZone.getDefault().id})",
                isTrackingActive = report.trackingActive
            )
        }
    }
}

