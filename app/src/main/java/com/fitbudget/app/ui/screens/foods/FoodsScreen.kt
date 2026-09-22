package com.fitbudget.app.ui.screens.foods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.domain.model.FoodCategory
import com.fitbudget.app.domain.model.FoodRole
import com.fitbudget.app.ui.components.ConfirmDialog
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.PlainTextField
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.util.Formatters
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsScreen(
    viewModel: FoodsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FoodEntity?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food database") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showRestoreConfirm = true }) { Text("Restore") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.startNewFood()
                    showEditor = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add food") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PlainTextField(
                    label = "Search foods",
                    value = state.query,
                    onValueChange = viewModel::onQueryChange
                )
            }

            item {
                SectionHeader(
                    "${state.foods.size} foods",
                    subtitle = "${state.customCount} added by you · " +
                        "${state.excludedKeys.size} excluded from suggestions"
                )
            }

            items(state.foods, key = { it.id }) { food ->
                val excluded = food.nameKey in state.excludedKeys
                FoodRow(
                    food = food,
                    excluded = excluded,
                    onToggleExclusion = { viewModel.toggleExclusion(food) },
                    onEdit = {
                        viewModel.startEditing(food)
                        showEditor = true
                    },
                    onDelete = { pendingDelete = food }
                )
            }

            item {
                Text(
                    "Calorie, protein and cost values are approximate reference figures — edit any food " +
                        "to match what you actually eat and pay.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (form.isEditing) "Edit food" else "Add a food") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PlainTextField(
                        label = "Name",
                        value = form.name,
                        onValueChange = { value -> viewModel.updateForm { it.copy(name = value) } },
                        errorMessage = form.errors["name"]
                    )
                    Spacer(Modifier.height(6.dp))
                    PlainTextField(
                        label = "Serving",
                        value = form.servingLabel,
                        onValueChange = { value -> viewModel.updateForm { it.copy(servingLabel = value) } },
                        errorMessage = form.errors["serving"],
                        supportingText = "e.g. 1 bowl (150 g)"
                    )
                    Spacer(Modifier.height(6.dp))
                    NumberField(
                        label = "Calories per serving",
                        value = form.calories,
                        onValueChange = { value -> viewModel.updateForm { it.copy(calories = value) } },
                        suffix = "kcal",
                        decimal = false,
                        errorMessage = form.errors["calories"]
                    )
                    NumberField(
                        label = "Protein per serving",
                        value = form.protein,
                        onValueChange = { value -> viewModel.updateForm { it.copy(protein = value) } },
                        suffix = "g",
                        errorMessage = form.errors["protein"]
                    )
                    NumberField(
                        label = "Cost per serving",
                        value = form.cost,
                        onValueChange = { value -> viewModel.updateForm { it.copy(cost = value) } },
                        suffix = "₹",
                        errorMessage = form.errors["cost"]
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FoodCategory.entries.forEach { category ->
                            FilterChip(
                                selected = form.category == category,
                                onClick = { viewModel.updateForm { it.copy(category = category) } },
                                label = { Text(category.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Role in a meal", style = MaterialTheme.typography.labelLarge)
                    Column {
                        FoodRole.entries.chunked(4).forEach { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                chunk.forEach { role ->
                                    FilterChip(
                                        selected = form.role == role,
                                        onClick = { viewModel.updateForm { it.copy(role = role) } },
                                        label = {
                                            Text(
                                                role.name.lowercase()
                                                    .replaceFirstChar { c -> c.uppercase() }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.save { showEditor = false } },
                    enabled = !form.saving,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("Cancel") }
            }
        )
    }

    pendingDelete?.let { food ->
        ConfirmDialog(
            title = "Delete ${food.name}?",
            message = if (food.isCustom)
                "This food will be removed from your database. Meals already logged keep their values."
            else "This is a built-in food. You can restore the built-in list later from this screen.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.delete(food)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = "Restore built-in foods?",
            message = "The bundled Indian food list will be reinstalled. Foods you added yourself will be removed.",
            confirmLabel = "Restore",
            destructive = true,
            onConfirm = {
                viewModel.restoreSeedFoods()
                showRestoreConfirm = false
            },
            onDismiss = { showRestoreConfirm = false }
        )
    }
}

@Composable
private fun FoodRow(
    food: FoodEntity,
    excluded: Boolean,
    onToggleExclusion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    FitCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        food.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (food.isCustom) {
                        Spacer(Modifier.height(0.dp))
                        Text(
                            "  · yours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "${food.servingLabel} · ${food.calories.roundToInt()} kcal · " +
                        "${Formatters.grams(food.proteinG)} protein · ${Formatters.rupees(food.costRupees)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${food.category.label} · ${food.role.name.lowercase()} · " +
                        "%.2f g protein per ₹".format(food.proteinPerRupee),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (excluded) {
                    Text(
                        "Excluded from suggestions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Food options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (excluded) "Allow in suggestions" else "Never suggest") },
                        onClick = { menuOpen = false; onToggleExclusion() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}
