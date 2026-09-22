package com.fitbudget.app.ui.screens.budget

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.ExpenseEntity
import com.fitbudget.app.data.repository.BudgetRepository
import com.fitbudget.app.data.repository.DayRepository
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.repository.StatsRepository
import com.fitbudget.app.domain.BudgetCalculator
import com.fitbudget.app.domain.BudgetSnapshot
import com.fitbudget.app.domain.DaySummary
import com.fitbudget.app.domain.MonthlyBudgetSummary
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetUiState(
    val loading: Boolean = true,
    val epochDay: Long = DateTimeUtils.todayEpochDay(),
    val month: YearMonth = YearMonth.now(),
    val budget: Double = 0.0,
    val spent: Double = 0.0,
    val plannedCost: Double = 0.0,
    val expenses: List<ExpenseEntity> = emptyList(),
    val monthSummaries: List<DaySummary> = emptyList(),
    val monthly: MonthlyBudgetSummary = MonthlyBudgetSummary(0, 0.0, 0.0, 0, 0),
    val budgetStreak: Int = 0
) {
    val snapshot: BudgetSnapshot
        get() = BudgetCalculator.snapshot(budget, spent, (plannedCost - spent).coerceAtLeast(0.0))
    val spendSeries: List<Pair<Long, Double>>
        get() = monthSummaries.map { it.epochDay to it.spent }
}

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val profileRepository: ProfileRepository,
    private val dayRepository: DayRepository,
    private val statsRepository: StatsRepository
) : BaseViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BudgetUiState> = combine(
        currentDayFlow(),
        month
    ) { day, selectedMonth -> day to selectedMonth }
        .flatMapLatest { (day, selectedMonth) ->
            combine(
                statsRepository.observeSummary(day),
                budgetRepository.observeExpenses(day)
            ) { summary, expenses -> summary to expenses }
                .map { (summary, expenses) ->
                    val range = DateTimeUtils.monthRange(selectedMonth)
                    val summaries = statsRepository.daySummaries(range.first, range.last)
                    BudgetUiState(
                        loading = false,
                        epochDay = day,
                        month = selectedMonth,
                        budget = summary.budget,
                        spent = summary.spent,
                        plannedCost = summary.plannedCost,
                        expenses = expenses,
                        monthSummaries = summaries,
                        monthly = BudgetCalculator.monthly(summaries),
                        budgetStreak = statsRepository.streaks(lookBackDays = 120).budget
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState()
        )

    fun showPreviousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun showNextMonth() {
        val next = month.value.plusMonths(1)
        if (next.isAfter(YearMonth.now())) return
        month.value = next
    }

    fun addExpense(label: String, rawAmount: String) {
        val parsed = Validators.parseDecimal(rawAmount)
        val result = Validators.cost(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid amount.")
            return
        }
        viewModelScope.launch {
            runCatching {
                budgetRepository.addExpense(label, parsed!!, uiState.value.epochDay)
                notifyUser("Added ₹${parsed.toInt()} to today's spending.")
            }.onFailure { notifyError(it, "Could not add that expense.") }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            runCatching {
                budgetRepository.delete(expense)
                notifyUser("Expense removed.")
            }.onFailure { notifyError(it, "Could not remove that expense.") }
        }
    }

    fun setDailyBudget(rawValue: String) {
        val parsed = Validators.parseDecimal(rawValue)
        val result = Validators.budget(parsed)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid budget.")
            return
        }
        viewModelScope.launch {
            runCatching {
                profileRepository.setDailyBudget(parsed!!)
                dayRepository.applyBudgetToToday(parsed)
                notifyUser("Daily budget set to ₹${parsed.toInt()}.")
            }.onFailure { notifyError(it, "Could not update the budget.") }
        }
    }
}
