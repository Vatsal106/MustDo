package com.example.todo.features.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.features.reports.domain.MonthlyReport
import com.example.todo.features.reports.domain.ReportCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class MonthlyReportUiState(
    val report: MonthlyReport? = null,
    val isLoading: Boolean = true,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) // 0-indexed
)

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val reportCalculator: ReportCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState: StateFlow<MonthlyReportUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    private fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            val report = reportCalculator.generateMonthlyReport(state.currentYear, state.currentMonth)
            _uiState.update { 
                it.copy(
                    report = report,
                    isLoading = false
                )
            }
        }
    }

    fun previousMonth() {
        val state = _uiState.value
        val newMonth = if (state.currentMonth == 0) 11 else state.currentMonth - 1
        val newYear = if (state.currentMonth == 0) state.currentYear - 1 else state.currentYear
        _uiState.update { it.copy(currentMonth = newMonth, currentYear = newYear) }
        loadReport()
    }

    fun nextMonth() {
        val state = _uiState.value
        val newMonth = if (state.currentMonth == 11) 0 else state.currentMonth + 1
        val newYear = if (state.currentMonth == 11) state.currentYear + 1 else state.currentYear
        _uiState.update { it.copy(currentMonth = newMonth, currentYear = newYear) }
        loadReport()
    }
}
