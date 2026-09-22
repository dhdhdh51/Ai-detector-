package com.fitbudget.app.ui.screens.report

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.repository.BackupRepository
import com.fitbudget.app.data.repository.MonthlyReport
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth

data class MonthlyReportUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val report: MonthlyReport? = null,
    val canGoForward: Boolean = false
)

class MonthlyReportViewModel(
    private val statsRepository: StatsRepository,
    private val backupRepository: BackupRepository
) : BaseViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val _pendingShareFile = MutableStateFlow<File?>(null)
    val pendingShareFile: StateFlow<File?> = _pendingShareFile.asStateFlow()

    val uiState: StateFlow<MonthlyReportUiState> = month
        .map { selected ->
            MonthlyReportUiState(
                loading = false,
                month = selected,
                report = statsRepository.monthlyReport(selected),
                canGoForward = selected.isBefore(YearMonth.now())
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyReportUiState()
        )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        val next = month.value.plusMonths(1)
        if (!next.isAfter(YearMonth.now())) month.value = next
    }

    fun shareReport() {
        viewModelScope.launch {
            runCatching { backupRepository.writeMonthlyReport(month.value) }
                .onSuccess { _pendingShareFile.value = it }
                .onFailure { notifyError(it, "Could not build the report file.") }
        }
    }

    fun consumeShare() {
        _pendingShareFile.value = null
    }

    fun reportText(): String = uiState.value.report?.toShareText().orEmpty()
}
