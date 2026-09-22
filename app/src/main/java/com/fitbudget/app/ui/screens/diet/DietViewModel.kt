package com.fitbudget.app.ui.screens.diet

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.data.repository.DietRepository
import com.fitbudget.app.data.repository.FoodRepository
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.MealType
import com.fitbudget.app.ui.BaseViewModel
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One meal section of the day (the four standard meals plus any custom meals). */
data class MealGroup(
    val key: String,
    val label: String,
    val mealType: MealType?,
    val timeMinutes: Int,
    val items: List<MealEntryEntity>
) {
    val calories: Double get() = items.sumOf { it.totalCalories }
    val protein: Double get() = items.sumOf { it.totalProtein }
    val cost: Double get() = items.sumOf { it.totalCost }
    val allCompleted: Boolean get() = items.isNotEmpty() && items.all { it.completed }
    val completedCount: Int get() = items.count { it.completed }
}

data class DietUiState(
    val loading: Boolean = true,
    val epochDay: Long = DateTimeUtils.todayEpochDay(),
    val groups: List<MealGroup> = emptyList(),
    val calorieTarget: Double = 0.0,
    val proteinTarget: Double = 0.0,
    val budget: Double = 0.0,
    val dietPreference: DietPreference = DietPreference.VEGETARIAN
) {
    val isToday: Boolean get() = epochDay == DateTimeUtils.todayEpochDay()
    val isFuture: Boolean get() = epochDay > DateTimeUtils.todayEpochDay()
    val dayLabel: String get() = DateTimeUtils.relativeDayLabel(epochDay)
    val totalCalories: Double get() = groups.sumOf { it.calories }
    val totalProtein: Double get() = groups.sumOf { it.protein }
    val totalCost: Double get() = groups.sumOf { it.cost }
    val completedCost: Double
        get() = groups.sumOf { group -> group.items.filter { it.completed }.sumOf { it.totalCost } }
    val completedCalories: Double
        get() = groups.sumOf { group -> group.items.filter { it.completed }.sumOf { it.totalCalories } }
    val isEmpty: Boolean get() = !loading && groups.isEmpty()
    val overBudget: Boolean get() = budget > 0 && totalCost > budget
}

class DietViewModel(
    private val dietRepository: DietRepository,
    private val foodRepository: FoodRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {

    private val selectedDay = MutableStateFlow(DateTimeUtils.todayEpochDay())
    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DietUiState> = selectedDay
        .flatMapLatest { day ->
            ensurePlan(day)
            combine(
                dietRepository.observeDay(day),
                profileRepository.profile
            ) { entries, profile ->
                DietUiState(
                    loading = false,
                    epochDay = day,
                    groups = entries.toGroups(),
                    calorieTarget = profile?.estimatedCalorieTarget ?: 0.0,
                    proteinTarget = profile?.proteinTargetGrams ?: 0.0,
                    budget = profile?.dailyBudget ?: 0.0,
                    dietPreference = profile?.dietPreference ?: DietPreference.VEGETARIAN
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DietUiState()
        )

    /** Food list for the picker sheet, filtered live by the search box. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val foods: StateFlow<List<FoodEntity>> = searchQuery
        .flatMapLatest { query -> foodRepository.search(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private fun List<MealEntryEntity>.toGroups(): List<MealGroup> {
        if (isEmpty()) return emptyList()
        val standard = MealType.entries.mapNotNull { type ->
            val items = filter { it.mealType == type }
            if (items.isEmpty()) {
                null
            } else {
                MealGroup(
                    key = type.name,
                    label = type.label,
                    mealType = type,
                    timeMinutes = items.minOf { it.timeMinutes },
                    items = items.sortedBy { it.sortOrder }
                )
            }
        }
        val custom = filter { it.mealType == null }
            .groupBy { it.mealLabel }
            .map { (label, items) ->
                MealGroup(
                    key = "CUSTOM:$label",
                    label = label,
                    mealType = null,
                    timeMinutes = items.minOf { it.timeMinutes },
                    items = items.sortedBy { it.sortOrder }
                )
            }
        return (standard + custom).sortedBy { it.timeMinutes }
    }

    private suspend fun ensurePlan(day: Long) {
        runCatching {
            if (profileRepository.get()?.onboardingComplete == true) {
                dietRepository.ensurePlanForDay(day)
            }
        }.onFailure { notifyError(it, "Could not prepare the plan for this day.") }
    }

    // ---------------------------------------------------------------- navigation

    fun showPreviousDay() {
        selectedDay.value = selectedDay.value - 1
    }

    fun showNextDay() {
        selectedDay.value = selectedDay.value + 1
    }

    fun showToday() {
        selectedDay.value = DateTimeUtils.todayEpochDay()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    // ---------------------------------------------------------------- mutations

    fun setItemCompleted(item: MealEntryEntity, completed: Boolean) {
        viewModelScope.launch {
            runCatching { dietRepository.setItemCompleted(item.id, completed) }
                .onFailure { notifyError(it, "Could not update that item.") }
        }
    }

    fun setMealCompleted(group: MealGroup, completed: Boolean) {
        viewModelScope.launch {
            runCatching {
                val type = group.mealType
                if (type != null) {
                    dietRepository.setMealCompleted(uiState.value.epochDay, type, completed)
                } else {
                    group.items.forEach { dietRepository.setItemCompleted(it.id, completed) }
                }
            }.onFailure { notifyError(it, "Could not update that meal.") }
        }
    }

    fun updateQuantity(item: MealEntryEntity, quantity: Double) {
        val result = Validators.quantity(quantity)
        if (!result.isValid) {
            notifyUser(result.message ?: "Invalid quantity.")
            return
        }
        viewModelScope.launch {
            runCatching { dietRepository.updateQuantity(item.id, quantity) }
                .onFailure { notifyError(it, "Could not change the quantity.") }
        }
    }

    fun deleteItem(item: MealEntryEntity) {
        viewModelScope.launch {
            runCatching {
                dietRepository.delete(item)
                notifyUser("${item.foodName} removed.")
            }.onFailure { notifyError(it, "Could not remove that item.") }
        }
    }

    fun replaceFood(item: MealEntryEntity, food: FoodEntity) {
        viewModelScope.launch {
            runCatching {
                dietRepository.replaceFood(item.id, food)
                notifyUser("Swapped for ${food.name}.")
            }.onFailure { notifyError(it, "Could not swap that food.") }
        }
    }

    fun addFood(group: MealGroup, food: FoodEntity, quantity: Double = 1.0) {
        viewModelScope.launch {
            runCatching {
                dietRepository.addFoodToMeal(
                    epochDay = uiState.value.epochDay,
                    mealType = group.mealType,
                    mealLabel = group.label,
                    timeMinutes = group.timeMinutes,
                    food = food,
                    quantity = quantity
                )
                notifyUser("${food.name} added to ${group.label}.")
            }.onFailure { notifyError(it, "Could not add that food.") }
        }
    }

    fun addCustomMeal(label: String, hour: Int, minute: Int, food: FoodEntity, quantity: Double = 1.0) {
        viewModelScope.launch {
            runCatching {
                dietRepository.addCustomMeal(
                    epochDay = uiState.value.epochDay,
                    label = label,
                    timeMinutes = DateTimeUtils.minutesOfDay(hour, minute),
                    food = food,
                    quantity = quantity
                )
                notifyUser("$label added.")
            }.onFailure { notifyError(it, "Could not add that meal.") }
        }
    }

    fun updateMealTime(group: MealGroup, hour: Int, minute: Int) {
        viewModelScope.launch {
            runCatching {
                dietRepository.updateMealTime(
                    epochDay = uiState.value.epochDay,
                    groupKey = group.key,
                    timeMinutes = DateTimeUtils.minutesOfDay(hour, minute)
                )
            }.onFailure { notifyError(it, "Could not change the meal time.") }
        }
    }

    fun regeneratePlan() {
        viewModelScope.launch {
            runCatching {
                dietRepository.regeneratePlan(uiState.value.epochDay)
                notifyUser("A fresh plan has been generated for ${uiState.value.dayLabel.lowercase()}.")
            }.onFailure { notifyError(it, "Could not build a new plan.") }
        }
    }

    fun clearDay() {
        viewModelScope.launch {
            runCatching {
                dietRepository.deleteDay(uiState.value.epochDay)
                notifyUser("Plan cleared.")
            }.onFailure { notifyError(it, "Could not clear the plan.") }
        }
    }

    fun excludeFood(food: FoodEntity) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.toggleExcludedFood(food.nameKey)
                notifyUser("${food.name} will not be suggested again.")
            }.onFailure { notifyError(it, "Could not update your food preferences.") }
        }
    }
}
