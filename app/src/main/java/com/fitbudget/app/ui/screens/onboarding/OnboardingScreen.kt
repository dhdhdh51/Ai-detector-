package com.fitbudget.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.R
import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.Gender
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.PlainTextField
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.components.TimeField
import com.fitbudget.app.util.DateTimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            OnboardingHeader(step = state.step, totalSteps = state.totalSteps)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (state.step) {
                    0 -> StepAboutYou(
                        state = state,
                        onNameChange = { name -> viewModel.update { it.copy(name = name) } },
                        onGenderChange = { gender -> viewModel.update { it.copy(gender = gender) } },
                        onPickDate = { showDatePicker = true }
                    )

                    1 -> StepBody(
                        state = state,
                        onHeightChange = { v -> viewModel.update { it.copy(heightCm = v) } },
                        onWeightChange = { v -> viewModel.update { it.copy(currentWeight = v) } },
                        onTargetChange = { v -> viewModel.update { it.copy(targetWeight = v) } }
                    )

                    2 -> StepBudgetAndFood(
                        state = state,
                        onBudgetChange = { v -> viewModel.update { it.copy(dailyBudget = v) } },
                        onActivityChange = { v -> viewModel.update { it.copy(activityLevel = v) } },
                        onDietChange = { v -> viewModel.update { it.copy(dietPreference = v) } }
                    )

                    else -> StepRoutine(
                        state = state,
                        onWakeChange = { minutes -> viewModel.update { it.copy(wakeMinutes = minutes) } },
                        onSleepChange = { minutes -> viewModel.update { it.copy(sleepMinutes = minutes) } }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.step > 0) {
                    TextButton(onClick = viewModel::back) { Text("Back") }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { viewModel.next(onFinished) },
                    enabled = !state.saving,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (state.isLastStep) "Create my plan" else "Continue")
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = state.form.dateOfBirth
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
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
private fun OnboardingHeader(step: Int, totalSteps: Int) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)) {
        Text(
            "FitBudget",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            if (index <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun StepAboutYou(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onPickDate: () -> Unit
) {
    SectionHeader("About you", subtitle = "Used only on this device to estimate your plan.")
    PlainTextField(
        label = "Your name",
        value = state.form.name,
        onValueChange = onNameChange,
        errorMessage = state.errors["name"]
    )
    FitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPickDate,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Date of birth", style = MaterialTheme.typography.bodyMedium)
                Text(
                    DateTimeUtils.formatFullDate(state.form.dateOfBirth),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "${ageOf(state.form.dateOfBirth)} yrs",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    state.errors["dob"]?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }

    Text("Gender", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Gender.entries.forEach { gender ->
            FilterChip(
                selected = state.form.gender == gender,
                onClick = { onGenderChange(gender) },
                label = { Text(gender.label) }
            )
        }
    }
    Text(
        "Gender is only used to pick the right formula for the calorie estimate.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StepBody(
    state: OnboardingUiState,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onTargetChange: (String) -> Unit
) {
    SectionHeader("Your numbers", subtitle = "You can change all of these later.")
    NumberField(
        label = "Height",
        value = state.form.heightCm,
        onValueChange = onHeightChange,
        suffix = "cm",
        errorMessage = state.errors["height"],
        supportingText = "Between 100 and 250 cm"
    )
    NumberField(
        label = "Current weight",
        value = state.form.currentWeight,
        onValueChange = onWeightChange,
        suffix = "kg",
        errorMessage = state.errors["weight"],
        supportingText = "Between 30 and 300 kg"
    )
    NumberField(
        label = "Target weight",
        value = state.form.targetWeight,
        onValueChange = onTargetChange,
        suffix = "kg",
        errorMessage = state.errors["target"],
        supportingText = "Must be lower than or equal to your current weight"
    )
}

@Composable
private fun StepBudgetAndFood(
    state: OnboardingUiState,
    onBudgetChange: (String) -> Unit,
    onActivityChange: (ActivityLevel) -> Unit,
    onDietChange: (DietPreference) -> Unit
) {
    SectionHeader("Food budget & activity", subtitle = "This is what keeps the plan affordable.")
    NumberField(
        label = "Daily food budget",
        value = state.form.dailyBudget,
        onValueChange = onBudgetChange,
        suffix = "₹",
        errorMessage = state.errors["budget"],
        supportingText = "Between ₹1 and ₹10,000 a day",
        decimal = false
    )

    Text("Food preference", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DietPreference.entries.forEach { preference ->
            FilterChip(
                selected = state.form.dietPreference == preference,
                onClick = { onDietChange(preference) },
                label = { Text(preference.label) }
            )
        }
    }

    Spacer(Modifier.height(4.dp))
    Text("Activity level", style = MaterialTheme.typography.labelLarge)
    ActivityLevel.entries.forEach { level ->
        FitCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onActivityChange(level) },
            containerColor = if (state.form.activityLevel == level)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(level.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    level.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepRoutine(
    state: OnboardingUiState,
    onWakeChange: (Int) -> Unit,
    onSleepChange: (Int) -> Unit
) {
    SectionHeader(
        "Your daily rhythm",
        subtitle = "Water reminders are spread between these two times."
    )
    TimeField(
        label = "Wake-up time",
        hour = DateTimeUtils.hourOf(state.form.wakeMinutes),
        minute = DateTimeUtils.minuteOf(state.form.wakeMinutes),
        onTimeSelected = { hour, minute -> onWakeChange(DateTimeUtils.minutesOfDay(hour, minute)) }
    )
    TimeField(
        label = "Sleep time",
        hour = DateTimeUtils.hourOf(state.form.sleepMinutes),
        minute = DateTimeUtils.minuteOf(state.form.sleepMinutes),
        onTimeSelected = { hour, minute -> onSleepChange(DateTimeUtils.minutesOfDay(hour, minute)) }
    )
    Text(
        "All calculations in FitBudget are estimates, not medical advice.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun ageOf(dateOfBirth: LocalDate): Int =
    com.fitbudget.app.domain.HealthCalculator.age(dateOfBirth)
