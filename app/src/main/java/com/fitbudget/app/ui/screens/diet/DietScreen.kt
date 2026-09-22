package com.fitbudget.app.ui.screens.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.ui.components.DisclaimerCard
import com.fitbudget.app.ui.components.EmptyState
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.LabeledProgressBar
import com.fitbudget.app.ui.components.PlainTextField
import com.fitbudget.app.ui.components.QuantityStepper
import com.fitbudget.app.ui.components.TimeField
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters
import kotlin.math.roundToInt

private sealed interface PickerTarget {
    data class AddToMeal(val group: MealGroup) : PickerTarget
    data class ReplaceItem(val item: MealEntryEntity) : PickerTarget
    data object CustomMeal : PickerTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    viewModel: DietViewModel,
    contentPadding: PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()

    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var quantityItem by remember { mutableStateOf<MealEntryEntity?>(null) }
    var customMealFood by remember { mutableStateOf<FoodEntity?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DayHeader(
                dayLabel = state.dayLabel,
                dateLabel = DateTimeUtils.formatFullDate(DateTimeUtils.date(state.epochDay)),
                onPrevious = viewModel::showPreviousDay,
                onNext = viewModel::showNextDay,
                onToday = viewModel::showToday,
                showToday = !state.isToday
            )
        }

        item {
            DayTotalsCard(state = state)
        }

        if (state.isEmpty) {
            item {
                EmptyState(
                    icon = Icons.Default.NoFood,
                    title = "No meals planned",
                    message = "Generate an affordable plan from your food database, or add meals yourself.",
                    actionLabel = "Generate plan",
                    onAction = viewModel::regeneratePlan
                )
            }
        }

        items(state.groups, key = { it.key }) { group ->
            MealSection(
                group = group,
                onToggleMeal = { completed -> viewModel.setMealCompleted(group, completed) },
                onToggleItem = { item, completed -> viewModel.setItemCompleted(item, completed) },
                onEditQuantity = { quantityItem = it },
                onReplace = { pickerTarget = PickerTarget.ReplaceItem(it) },
                onDelete = viewModel::deleteItem,
                onExclude = { item ->
                    foods.firstOrNull { it.id == item.foodId }?.let(viewModel::excludeFood)
                },
                onAddFood = { pickerTarget = PickerTarget.AddToMeal(group) },
                onTimeChange = { hour, minute -> viewModel.updateMealTime(group, hour, minute) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { pickerTarget = PickerTarget.CustomMeal },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Custom meal")
                }
                OutlinedButton(
                    onClick = viewModel::regeneratePlan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New plan")
                }
            }
        }

        if (state.groups.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear this day's plan", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item { DisclaimerCard() }
        item { Spacer(Modifier.height(8.dp)) }
    }

    // ---- food picker sheet ----
    val target = pickerTarget
    if (target != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                pickerTarget = null
                viewModel.onSearchQueryChange("")
            },
            sheetState = sheetState
        ) {
            FoodPickerContent(
                foods = foods,
                title = when (target) {
                    is PickerTarget.AddToMeal -> "Add to ${target.group.label}"
                    is PickerTarget.ReplaceItem -> "Replace ${target.item.foodName}"
                    PickerTarget.CustomMeal -> "Pick a food for your meal"
                },
                onSearch = viewModel::onSearchQueryChange,
                onSelect = { food ->
                    when (target) {
                        is PickerTarget.AddToMeal -> {
                            viewModel.addFood(target.group, food)
                            pickerTarget = null
                        }

                        is PickerTarget.ReplaceItem -> {
                            viewModel.replaceFood(target.item, food)
                            pickerTarget = null
                        }

                        PickerTarget.CustomMeal -> {
                            customMealFood = food
                            pickerTarget = null
                        }
                    }
                    viewModel.onSearchQueryChange("")
                }
            )
        }
    }

    // ---- quantity dialog ----
    quantityItem?.let { item ->
        var quantity by remember(item.id) { mutableDoubleStateOf(item.quantity) }
        AlertDialog(
            onDismissRequest = { quantityItem = null },
            title = { Text(item.foodName) },
            text = {
                Column {
                    Text(
                        "1 serving = ${item.servingLabel} · ${item.caloriesPerServing.roundToInt()} kcal · ${Formatters.rupees(item.costPerServing)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    QuantityStepper(
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${(item.caloriesPerServing * quantity).roundToInt()} kcal · " +
                            "${Formatters.grams(item.proteinPerServing * quantity)} protein · " +
                            Formatters.rupees(item.costPerServing * quantity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateQuantity(item, quantity)
                    quantityItem = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { quantityItem = null }) { Text("Cancel") }
            }
        )
    }

    // ---- custom meal dialog ----
    customMealFood?.let { food ->
        var label by remember { mutableStateOf("Extra meal") }
        var hour by remember { mutableIntStateOf(16) }
        var minute by remember { mutableIntStateOf(0) }
        var quantity by remember { mutableDoubleStateOf(1.0) }
        AlertDialog(
            onDismissRequest = { customMealFood = null },
            title = { Text("New meal") },
            text = {
                Column {
                    PlainTextField(
                        label = "Meal name",
                        value = label,
                        onValueChange = { label = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    TimeField(
                        label = "Meal time",
                        hour = hour,
                        minute = minute,
                        onTimeSelected = { h, m -> hour = h; minute = m }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("${food.name} · ${food.servingLabel}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    QuantityStepper(quantity = quantity, onQuantityChange = { quantity = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addCustomMeal(label, hour, minute, food, quantity)
                    customMealFood = null
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { customMealFood = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        com.fitbudget.app.ui.components.ConfirmDialog(
            title = "Clear this plan?",
            message = "Every meal logged for ${state.dayLabel.lowercase()} will be removed. This cannot be undone.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = {
                viewModel.clearDay()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun DayHeader(
    dayLabel: String,
    dateLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    showToday: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(dayLabel, style = MaterialTheme.typography.titleLarge)
            Text(
                dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showToday) {
                TextButton(onClick = onToday) { Text("Back to today") }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun DayTotalsCard(state: DietUiState) {
    FitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    tint = FitAccent.diet,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Planned today", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    "${state.totalCalories.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))
            LabeledProgressBar(
                label = "Calories eaten",
                valueText = "${state.completedCalories.roundToInt()} / ${state.calorieTarget.roundToInt()} kcal",
                fraction = if (state.calorieTarget > 0)
                    (state.completedCalories / state.calorieTarget).toFloat() else 0f,
                color = FitAccent.diet
            )
            Spacer(Modifier.height(12.dp))
            LabeledProgressBar(
                label = "Protein eaten",
                valueText = "${state.groups.sumOf { g -> g.items.filter { it.completed }.sumOf { it.totalProtein } }.roundToInt()} / ${state.proteinTarget.roundToInt()} g",
                fraction = if (state.proteinTarget > 0)
                    (state.groups.sumOf { g -> g.items.filter { it.completed }.sumOf { it.totalProtein } } / state.proteinTarget).toFloat()
                else 0f,
                color = FitAccent.weight
            )
            Spacer(Modifier.height(12.dp))
            LabeledProgressBar(
                label = if (state.overBudget) "Planned cost is over budget" else "Planned cost",
                valueText = "${Formatters.rupees(state.totalCost)} / ${Formatters.rupees(state.budget)}",
                fraction = if (state.budget > 0) (state.totalCost / state.budget).toFloat() else 0f,
                color = if (state.overBudget) MaterialTheme.colorScheme.error else FitAccent.budget
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Spent so far (ticked off): ${Formatters.rupees(state.completedCost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MealSection(
    group: MealGroup,
    onToggleMeal: (Boolean) -> Unit,
    onToggleItem: (MealEntryEntity, Boolean) -> Unit,
    onEditQuantity: (MealEntryEntity) -> Unit,
    onReplace: (MealEntryEntity) -> Unit,
    onDelete: (MealEntryEntity) -> Unit,
    onExclude: (MealEntryEntity) -> Unit,
    onAddFood: () -> Unit,
    onTimeChange: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    FitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = group.allCompleted,
                    onCheckedChange = onToggleMeal
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${DateTimeUtils.formatMinutesOfDay(group.timeMinutes)} · " +
                            "${group.calories.roundToInt()} kcal · " +
                            "${Formatters.grams(group.protein)} protein · " +
                            Formatters.rupees(group.cost),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showTimePicker = true }) { Text("Time") }
            }

            group.items.forEach { item ->
                MealItemRow(
                    item = item,
                    onToggle = { onToggleItem(item, it) },
                    onEditQuantity = { onEditQuantity(item) },
                    onReplace = { onReplace(item) },
                    onDelete = { onDelete(item) },
                    onExclude = { onExclude(item) }
                )
            }

            TextButton(
                onClick = onAddFood,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add food")
            }
        }
    }

    if (showTimePicker) {
        com.fitbudget.app.ui.components.FitTimePickerDialog(
            initialHour = DateTimeUtils.hourOf(group.timeMinutes),
            initialMinute = DateTimeUtils.minuteOf(group.timeMinutes),
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                showTimePicker = false
                onTimeChange(hour, minute)
            },
            title = "${group.label} time"
        )
    }
}

@Composable
private fun MealItemRow(
    item: MealEntryEntity,
    onToggle: (Boolean) -> Unit,
    onEditQuantity: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onExclude: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.completed, onCheckedChange = onToggle)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${Formatters.quantity(item.quantity)} × ${item.foodName}",
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (item.completed) TextDecoration.LineThrough else null
            )
            Text(
                "${item.servingLabel} · ${item.totalCalories.roundToInt()} kcal · " +
                    "${Formatters.grams(item.totalProtein)} · ${Formatters.rupees(item.totalCost)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Item options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Change quantity") },
                    onClick = { menuOpen = false; onEditQuantity() }
                )
                DropdownMenuItem(
                    text = { Text("Replace food") },
                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    onClick = { menuOpen = false; onReplace() }
                )
                DropdownMenuItem(
                    text = { Text("Never suggest this") },
                    onClick = { menuOpen = false; onExclude() }
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun FoodPickerContent(
    foods: List<FoodEntity>,
    title: String,
    onSearch: (String) -> Unit,
    onSelect: (FoodEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        PlainTextField(
            label = "Search foods",
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            }
        )
        Spacer(Modifier.height(8.dp))
        if (foods.isEmpty()) {
            Text(
                "No foods match \"$query\". Add it in Profile → Food database.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(foods, key = { it.id }) { food ->
                    ListItem(
                        headlineContent = { Text(food.name) },
                        supportingContent = {
                            Text(
                                "${food.servingLabel} · ${food.calories.roundToInt()} kcal · " +
                                    "${Formatters.grams(food.proteinG)} protein · ${Formatters.rupees(food.costRupees)}"
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = { onSelect(food) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Add") }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
