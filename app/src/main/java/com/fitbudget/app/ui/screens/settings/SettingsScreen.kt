package com.fitbudget.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.ThemeMode
import com.fitbudget.app.domain.model.UnitSystem
import com.fitbudget.app.ui.components.ConfirmDialog
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.util.Formatters
import com.fitbudget.app.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenFoods: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onDataReset: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingShare by viewModel.pendingShare.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let(viewModel::restoreBackup)
                ?: viewModel.restoreBackup("")
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(pendingShare) {
        pendingShare?.let { request ->
            ShareUtils.shareFile(context, request.file, request.mimeType, request.title)
            viewModel.consumeShare()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.busy) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            item { SectionHeader("Goals") }
            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingRow(
                            title = "Daily food budget",
                            value = Formatters.rupees(state.profile?.dailyBudget ?: 0.0),
                            onClick = { editing = EditTarget.Budget }
                        )
                        SettingRow(
                            title = "Target weight",
                            value = Formatters.kg(state.profile?.targetWeightKg ?: 0.0),
                            onClick = { editing = EditTarget.TargetWeight }
                        )
                        SettingRow(
                            title = "Calorie target",
                            value = state.profile?.calorieTargetOverride?.let { "${it.toInt()} kcal (manual)" }
                                ?: "${(state.profile?.estimatedCalorieTarget ?: 0.0).toInt()} kcal (estimated)",
                            onClick = { editing = EditTarget.Calories }
                        )
                        SettingRow(
                            title = "Water target",
                            value = Formatters.litres(state.settings.waterTargetMl),
                            onClick = { editing = EditTarget.Water }
                        )
                        SettingRow(
                            title = "Step goal",
                            value = Formatters.steps(state.settings.stepGoal),
                            onClick = { editing = EditTarget.Steps }
                        )
                    }
                }
            }

            item { SectionHeader("Food preferences") }
            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Plans only suggest foods that match this preference.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DietPreference.entries.forEach { preference ->
                                FilterChip(
                                    selected = state.profile?.dietPreference == preference,
                                    onClick = { viewModel.setDietPreference(preference) },
                                    label = { Text(preference.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenFoods,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                if (state.settings.excludedFoodKeys.isEmpty())
                                    "Food database & exclusions"
                                else "Food database · ${state.settings.excludedFoodKeys.size} excluded"
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Reminders & notifications") }
            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SwitchRow(
                            title = "All reminders",
                            subtitle = if (state.settings.remindersEnabled)
                                "Individual reminders can still be switched off"
                            else "Every reminder is paused",
                            checked = state.settings.remindersEnabled,
                            onCheckedChange = viewModel::setRemindersEnabled
                        )
                        SwitchRow(
                            title = "Notification sound",
                            subtitle = "Applies to every reminder",
                            checked = state.settings.notificationSoundEnabled,
                            onCheckedChange = viewModel::setNotificationSound
                        )
                        SwitchRow(
                            title = "Vibration",
                            subtitle = "Applies to every reminder",
                            checked = state.settings.notificationVibrationEnabled,
                            onCheckedChange = viewModel::setNotificationVibration
                        )
                        SettingRow(
                            title = "Reminder times",
                            value = "9 reminders",
                            onClick = onOpenReminders
                        )
                        if (!state.exactAlarmsAllowed) {
                            Text(
                                "Exact alarms are blocked by the system, so reminders may arrive a few minutes late.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Appearance") }
            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Theme", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = state.settings.themeMode == mode,
                                    onClick = { viewModel.setTheme(mode) },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Units", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnitSystem.entries.forEach { units ->
                                FilterChip(
                                    selected = state.settings.unitSystem == units,
                                    onClick = { viewModel.setUnits(units) },
                                    label = { Text(units.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(
                            title = "Use wallpaper colours",
                            subtitle = "Material You dynamic colour (Android 12+)",
                            checked = state.settings.dynamicColorEnabled,
                            onCheckedChange = viewModel::setDynamicColor
                        )
                    }
                }
            }

            item { SectionHeader("Your data", subtitle = "Everything stays on this device.") }
            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = viewModel::exportCsv,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Export data (CSV)") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.exportMonthlyReport() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Share this month's report") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::createBackup,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Create backup file (JSON)") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Restore from backup") }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Restoring replaces the logs on this device with the contents of the backup file.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Reset all data", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item { SectionHeader("About") }
            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingRow(title = "Privacy", value = "", onClick = onOpenPrivacy)
                        SettingRow(title = "About FitBudget", value = "", onClick = onOpenAbout)
                    }
                }
            }
        }
    }

    editing?.let { target ->
        EditValueDialog(
            target = target,
            state = state,
            onDismiss = { editing = null },
            onSave = { value ->
                when (target) {
                    EditTarget.Budget -> viewModel.setDailyBudget(value)
                    EditTarget.TargetWeight -> viewModel.setTargetWeight(value)
                    EditTarget.Water -> viewModel.setWaterTarget(value)
                    EditTarget.Steps -> viewModel.setStepGoal(value)
                    EditTarget.Calories -> viewModel.setCalorieOverride(value)
                }
                editing = null
            }
        )
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = "Erase all FitBudget data?",
            message = "Your profile, weight log, meals, water, workouts, steps, expenses and reminder " +
                "settings will be permanently deleted from this device. Export a backup first if you " +
                "want to keep them.",
            confirmLabel = "Erase everything",
            destructive = true,
            onConfirm = {
                showResetConfirm = false
                viewModel.resetAllData(onDataReset)
            },
            onDismiss = { showResetConfirm = false }
        )
    }
}

private enum class EditTarget { Budget, TargetWeight, Water, Steps, Calories }

@Composable
private fun EditValueDialog(
    target: EditTarget,
    state: SettingsUiState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val initial = when (target) {
        EditTarget.Budget -> (state.profile?.dailyBudget ?: 100.0).toInt().toString()
        EditTarget.TargetWeight -> "%.1f".format(state.profile?.targetWeightKg ?: 75.0)
        EditTarget.Water -> state.settings.waterTargetMl.toString()
        EditTarget.Steps -> state.settings.stepGoal.toString()
        EditTarget.Calories -> state.profile?.calorieTargetOverride?.toInt()?.toString().orEmpty()
    }
    var value by remember { mutableStateOf(initial) }

    val config = when (target) {
        EditTarget.Budget -> Triple("Daily food budget", "₹", "Between ₹1 and ₹10,000")
        EditTarget.TargetWeight -> Triple("Target weight", "kg", "Must be at or below your current weight")
        EditTarget.Water -> Triple("Water target", "ml", "Between 500 ml and 10,000 ml")
        EditTarget.Steps -> Triple("Step goal", "steps", "Between 0 and 100,000")
        EditTarget.Calories -> Triple("Calorie target", "kcal", "Leave blank to use the estimate")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(config.first) },
        text = {
            NumberField(
                label = config.first,
                value = value,
                onValueChange = { value = it },
                suffix = config.second,
                supportingText = config.third,
                decimal = target == EditTarget.TargetWeight
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (value.isNotEmpty()) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
