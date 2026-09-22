package com.fitbudget.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.Gender
import com.fitbudget.app.ui.components.DisclaimerCard
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.PlainTextField
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.components.StreakChip
import com.fitbudget.app.ui.components.TimeField
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigate: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val profile = state.profile

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    profile?.name?.takeIf { it.isNotBlank() } ?: "Your profile",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "${state.age} years · ${profile?.gender?.label ?: "—"} · " +
                        Formatters.height(profile?.heightCm ?: 0.0, state.settings.unitSystem),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FitCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estimates from your profile", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    MetricRow("BMI", "%.1f · %s".format(state.bmi, state.bmiCategory))
                    MetricRow(
                        "Healthy weight range",
                        "%.0f – %.0f kg".format(state.healthyRange.start, state.healthyRange.endInclusive)
                    )
                    MetricRow(
                        "Maintenance calories",
                        "${(profile?.tdee ?: 0.0).roundToInt()} kcal"
                    )
                    MetricRow(
                        "Daily target",
                        "${(profile?.estimatedCalorieTarget ?: 0.0).roundToInt()} kcal"
                    )
                    MetricRow(
                        "Protein target",
                        Formatters.grams(profile?.proteinTargetGrams ?: 0.0)
                    )
                    MetricRow("Progress to goal", "${(profile?.progressPercent ?: 0.0).roundToInt()}%")
                }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Streaks")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StreakChip("Diet", state.streaks.diet, modifier = Modifier.weight(1f))
                        StreakChip("Workout", state.streaks.workout, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StreakChip("Water", state.streaks.water, modifier = Modifier.weight(1f))
                        StreakChip("Budget", state.streaks.budget, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionHeader("Edit profile", subtitle = "Changes apply from today onwards.") }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlainTextField(
                        label = "Name",
                        value = form.name,
                        onValueChange = { value -> viewModel.update { it.copy(name = value) } },
                        errorMessage = form.errors["name"]
                    )
                    FitCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showDatePicker = true },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.height(0.dp))
                            Column(modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)) {
                                Text("Date of birth", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    DateTimeUtils.formatFullDate(form.dateOfBirth),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    form.errors["dob"]?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text("Gender", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Gender.entries.forEach { gender ->
                            FilterChip(
                                selected = form.gender == gender,
                                onClick = { viewModel.update { it.copy(gender = gender) } },
                                label = { Text(gender.label) }
                            )
                        }
                    }

                    NumberField(
                        label = "Height",
                        value = form.heightCm,
                        onValueChange = { value -> viewModel.update { it.copy(heightCm = value) } },
                        suffix = "cm",
                        errorMessage = form.errors["height"]
                    )
                    NumberField(
                        label = "Current weight",
                        value = form.currentWeight,
                        onValueChange = { value -> viewModel.update { it.copy(currentWeight = value) } },
                        suffix = "kg",
                        errorMessage = form.errors["weight"]
                    )
                    NumberField(
                        label = "Target weight",
                        value = form.targetWeight,
                        onValueChange = { value -> viewModel.update { it.copy(targetWeight = value) } },
                        suffix = "kg",
                        errorMessage = form.errors["target"]
                    )
                    NumberField(
                        label = "Daily food budget",
                        value = form.dailyBudget,
                        onValueChange = { value -> viewModel.update { it.copy(dailyBudget = value) } },
                        suffix = "₹",
                        decimal = false,
                        errorMessage = form.errors["budget"]
                    )
                    NumberField(
                        label = "Calorie target override (optional)",
                        value = form.calorieOverride,
                        onValueChange = { value -> viewModel.update { it.copy(calorieOverride = value) } },
                        suffix = "kcal",
                        decimal = false,
                        errorMessage = form.errors["calories"],
                        supportingText = "Leave blank to use the estimate from your profile."
                    )

                    Text("Food preference", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DietPreference.entries.forEach { preference ->
                            FilterChip(
                                selected = form.dietPreference == preference,
                                onClick = { viewModel.update { it.copy(dietPreference = preference) } },
                                label = { Text(preference.label) }
                            )
                        }
                    }

                    Text("Activity level", style = MaterialTheme.typography.labelLarge)
                    ActivityLevel.entries.forEach { level ->
                        FitCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.update { it.copy(activityLevel = level) } },
                            containerColor = if (form.activityLevel == level)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(level.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    level.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    TimeField(
                        label = "Wake-up time",
                        hour = DateTimeUtils.hourOf(form.wakeMinutes),
                        minute = DateTimeUtils.minuteOf(form.wakeMinutes),
                        onTimeSelected = { hour, minute ->
                            viewModel.update {
                                it.copy(wakeMinutes = DateTimeUtils.minutesOfDay(hour, minute))
                            }
                        }
                    )
                    TimeField(
                        label = "Sleep time",
                        hour = DateTimeUtils.hourOf(form.sleepMinutes),
                        minute = DateTimeUtils.minuteOf(form.sleepMinutes),
                        onTimeSelected = { hour, minute ->
                            viewModel.update {
                                it.copy(sleepMinutes = DateTimeUtils.minutesOfDay(hour, minute))
                            }
                        }
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = viewModel::save,
                            modifier = Modifier.weight(1f),
                            enabled = !form.saving,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Save changes") }
                        OutlinedButton(
                            onClick = viewModel::revert,
                            modifier = Modifier.weight(1f),
                            enabled = form.dirty && !form.saving,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Undo") }
                    }
                }
            }
        }

        item { SectionHeader("More") }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileLink("Settings", Icons.Default.Settings) { onNavigate("settings") }
                    ProfileLink("Reminders", Icons.Default.Notifications) { onNavigate("reminders") }
                    ProfileLink("Food database", Icons.Default.Restaurant) { onNavigate("foods") }
                    ProfileLink("Privacy", Icons.Default.Lock) { onNavigate("privacy") }
                    ProfileLink("About FitBudget", Icons.Default.Info) { onNavigate("about") }
                }
            }
        }

        item { DisclaimerCard() }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (showDatePicker) {
        val initialMillis = form.dateOfBirth.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.update { it.copy(dateOfBirth = date) }
                    }
                    showDatePicker = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState, title = null)
        }
    }
}

@Composable
private fun ProfileLink(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.height(0.dp))
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}
