package com.fitbudget.app.ui.screens.foods

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.repository.FoodRepository
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.Validators
import com.fitbudget.app.domain.model.FoodCategory
import com.fitbudget.app.domain.model.FoodRole
import com.fitbudget.app.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodForm(
    val name: String = "",
    val servingLabel: String = "1 serving",
    val calories: String = "",
    val protein: String = "",
    val cost: String = "",
    val category: FoodCategory = FoodCategory.VEG,
    val role: FoodRole = FoodRole.PROTEIN,
    val editingId: Long? = null,
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false
) {
    val isEditing: Boolean get() = editingId != null
}

data class FoodsUiState(
    val loading: Boolean = true,
    val foods: List<FoodEntity> = emptyList(),
    val excludedKeys: Set<String> = emptySet(),
    val query: String = ""
) {
    val customCount: Int get() = foods.count { it.isCustom }
}

class FoodsViewModel(
    private val foodRepository: FoodRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel() {

    private val query = MutableStateFlow("")
    private val _form = MutableStateFlow(FoodForm())
    val form: StateFlow<FoodForm> = _form.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FoodsUiState> = query
        .flatMapLatest { text ->
            combine(
                foodRepository.search(text),
                settingsRepository.settings
            ) { foods, settings ->
                FoodsUiState(
                    loading = false,
                    foods = foods,
                    excludedKeys = settings.excludedFoodKeys,
                    query = text
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FoodsUiState()
        )

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun updateForm(transform: (FoodForm) -> FoodForm) {
        _form.update { transform(it).copy(errors = emptyMap()) }
    }

    fun startNewFood() {
        _form.value = FoodForm()
    }

    fun startEditing(food: FoodEntity) {
        _form.value = FoodForm(
            name = food.name,
            servingLabel = food.servingLabel,
            calories = food.calories.toInt().toString(),
            protein = "%.1f".format(food.proteinG),
            cost = food.costRupees.toInt().toString(),
            category = food.category,
            role = food.role,
            editingId = food.id
        )
    }

    fun toggleExclusion(food: FoodEntity) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.toggleExcludedFood(food.nameKey)
                val excluded = food.nameKey in settingsRepository.current().excludedFoodKeys
                notifyUser(
                    if (excluded) "${food.name} will not be suggested in new plans."
                    else "${food.name} can be suggested again."
                )
            }.onFailure { notifyError(it, "Could not update your exclusions.") }
        }
    }

    fun save(onDone: () -> Unit) {
        val form = _form.value
        val calories = Validators.parseDecimal(form.calories)
        val protein = Validators.parseDecimal(form.protein)
        val cost = Validators.parseDecimal(form.cost)

        val errors = buildMap {
            Validators.foodName(form.name).message?.let { put("name", it) }
            Validators.servingLabel(form.servingLabel).message?.let { put("serving", it) }
            Validators.calories(calories).message?.let { put("calories", it) }
            Validators.protein(protein).message?.let { put("protein", it) }
            Validators.cost(cost).message?.let { put("cost", it) }
        }
        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return
        }

        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            val editingId = form.editingId
            val result = if (editingId != null) {
                val existing = foodRepository.getById(editingId)
                if (existing == null) {
                    Result.failure(IllegalStateException("That food no longer exists."))
                } else {
                    foodRepository.update(
                        existing.copy(
                            name = form.name.trim(),
                            servingLabel = form.servingLabel.trim(),
                            calories = calories!!,
                            proteinG = protein!!,
                            costRupees = cost!!,
                            category = form.category,
                            role = form.role
                        )
                    ).map { editingId }
                }
            } else {
                foodRepository.addCustom(
                    name = form.name,
                    servingLabel = form.servingLabel,
                    calories = calories!!,
                    proteinG = protein!!,
                    costRupees = cost!!,
                    category = form.category,
                    role = form.role
                )
            }

            _form.update { it.copy(saving = false) }
            result
                .onSuccess {
                    notifyUser(
                        if (editingId != null) "${form.name.trim()} updated."
                        else "${form.name.trim()} added to your food database."
                    )
                    _form.value = FoodForm()
                    onDone()
                }
                .onFailure { error ->
                    _form.update {
                        it.copy(errors = mapOf("name" to (error.message ?: "Could not save this food.")))
                    }
                }
        }
    }

    fun delete(food: FoodEntity) {
        viewModelScope.launch {
            runCatching {
                foodRepository.delete(food)
                notifyUser("${food.name} deleted.")
            }.onFailure { notifyError(it, "Could not delete that food.") }
        }
    }

    fun restoreSeedFoods() {
        viewModelScope.launch {
            runCatching {
                foodRepository.resetToSeed()
                notifyUser("Built-in food list restored. Your custom foods were removed.")
            }.onFailure { notifyError(it, "Could not restore the food list.") }
        }
    }
}
