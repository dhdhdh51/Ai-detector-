package com.fitbudget.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.domain.BudgetCalculator
import com.fitbudget.app.domain.HealthCalculator
import com.fitbudget.app.domain.WaterCalculator
import com.fitbudget.app.notifications.NotificationHelper
import com.fitbudget.app.ui.components.DisclaimerCard
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.LabeledProgressBar
import com.fitbudget.app.ui.components.MiniBarRow
import com.fitbudget.app.ui.components.ProgressRing
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.components.StatCard
import com.fitbudget.app.ui.components.StreakChip
import com.fitbudget.app.ui.navigation.Routes
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters
import androidx.compose.runtime.DisposableEffect
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsAllowed by remember { mutableStateOf(NotificationHelper.hasPermission(context)) }

    // Re-check the permission whenever the user comes back from system settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = NotificationHelper.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val profile = state.profile
    val summary = state.summary
    val units = state.settings.unitSystem

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.greeting.ifEmpty { DateTimeUtils.greeting() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        profile?.name?.takeIf { it.isNotBlank() } ?: "Welcome",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        DateTimeUtils.formatFullDate(DateTimeUtils.date(summary.epochDay)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        if (!notificationsAllowed) {
            item {
                ActionBanner(
                    title = "Reminders are switched off",
                    message = "FitBudget needs notification permission to remind you about meals, water and workouts.",
                    actionLabel = "Allow"
                ) {
                    runCatching {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        context.startActivity(intent)
                    }.onFailure {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    }
                }
            }
        }

        if (!state.exactAlarmsAllowed) {
            item {
                ActionBanner(
                    title = "Exact reminder times are blocked",
                    message = "Reminders will still arrive, but a few minutes late. Allow exact alarms for on-the-dot timing.",
                    actionLabel = "Allow"
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                            )
                        }
                    }
                }
            }
        }

        item {
            ProgressHeroCard(
                progressPercent = profile?.progressPercent ?: 0.0,
                startWeight = profile?.startWeightKg ?: 0.0,
                currentWeight = profile?.currentWeightKg ?: 0.0,
                targetWeight = profile?.targetWeightKg ?: 0.0,
                completionPercent = state.completionPercent,
                onClick = { onNavigate(Routes.PROGRESS) }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Current weight",
                    value = Formatters.weight(profile?.currentWeightKg ?: 0.0, units),
                    icon = Icons.Default.MonitorWeight,
                    accent = FitAccent.weight,
                    caption = "Tap to log today",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.WEIGHT) }
                )
                StatCard(
                    title = "Target weight",
                    value = Formatters.weight(profile?.targetWeightKg ?: 0.0, units),
                    icon = Icons.Default.MonitorHeart,
                    accent = FitAccent.diet,
                    caption = profile?.let {
                        if (it.remainingWeightKg > 0) "${Formatters.kg(it.remainingWeightKg)} to go"
                        else "Target reached"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "BMI (estimate)",
                    value = if ((profile?.bmi ?: 0.0) > 0) "%.1f".format(profile?.bmi) else "—",
                    icon = Icons.Default.MonitorHeart,
                    accent = FitAccent.workout,
                    caption = HealthCalculator.bmiCategory(profile?.bmi ?: 0.0),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Today's calories",
                    value = "${summary.caloriesConsumed.roundToInt()}",
                    icon = Icons.Default.Restaurant,
                    accent = FitAccent.diet,
                    caption = "of ${(profile?.estimatedCalorieTarget ?: 0.0).roundToInt()} kcal target",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.DIET) }
                )
            }
        }

        item {
            BudgetCard(
                budget = summary.budget,
                spent = summary.spent,
                onClick = { onNavigate(Routes.BUDGET) }
            )
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Today's trackers")
                    Spacer(Modifier.height(12.dp))

                    LabeledProgressBar(
                        label = "Water",
                        valueText = "${summary.waterMl} / ${summary.waterTargetMl} ml",
                        fraction = WaterCalculator.snapshot(summary.waterMl, summary.waterTargetMl)
                            .fraction.toFloat(),
                        color = FitAccent.water
                    )
                    Spacer(Modifier.height(12.dp))
                    LabeledProgressBar(
                        label = "Steps",
                        valueText = "${Formatters.steps(summary.steps)} / ${Formatters.steps(summary.stepGoal)}",
                        fraction = if (summary.stepGoal > 0)
                            (summary.steps.toFloat() / summary.stepGoal) else 0f,
                        color = FitAccent.steps
                    )
                    Spacer(Modifier.height(12.dp))
                    LabeledProgressBar(
                        label = "Meals ticked off",
                        valueText = "${summary.mealItemsCompleted} / ${summary.mealItemsPlanned}",
                        fraction = if (summary.mealItemsPlanned > 0)
                            summary.mealItemsCompleted.toFloat() / summary.mealItemsPlanned else 0f,
                        color = FitAccent.diet
                    )
                    Spacer(Modifier.height(12.dp))
                    LabeledProgressBar(
                        label = "Workout",
                        valueText = if (summary.workoutsCompleted > 0)
                            "${summary.workoutsCompleted} done" else "Not yet",
                        fraction = if (summary.workoutsCompleted > 0) 1f else 0f,
                        color = FitAccent.workout
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WaterCalculator.QUICK_AMOUNTS_ML.take(3).forEach { amount ->
                            OutlinedButton(
                                onClick = { viewModel.addWater(amount) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 4.dp,
                                    vertical = 10.dp
                                )
                            ) {
                                Icon(
                                    Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("$amount", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        "Daily checklist",
                        subtitle = "Today's progress: ${state.completionPercent}%"
                    )
                    Spacer(Modifier.height(8.dp))
                    state.checklist.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (item.done) "☑" else "☐",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (item.done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                item.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            item.detail?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Streaks", subtitle = "Only real completed days count.")
                    Spacer(Modifier.height(12.dp))
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

        if (state.weekSteps.isNotEmpty()) {
            item {
                FitCard(modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(Routes.STEPS) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("Last 7 days of steps")
                        Spacer(Modifier.height(10.dp))
                        MiniBarRow(
                            values = state.weekSteps,
                            goal = summary.stepGoal,
                            barColor = FitAccent.steps
                        )
                    }
                }
            }
        }

        item {
            FitCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your estimates", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Maintenance ≈ ${(profile?.tdee ?: 0.0).roundToInt()} kcal · " +
                            "suggested deficit ${state.deficitRange.first}–${state.deficitRange.last} kcal",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Estimated trend ≈ %.2f kg per week".format(state.weeklyTrendKg) +
                            (state.weeksToTarget?.let { " · about $it weeks to target" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item { DisclaimerCard() }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ProgressHeroCard(
    progressPercent: Double,
    startWeight: Double,
    currentWeight: Double,
    targetWeight: Double,
    completionPercent: Int,
    onClick: () -> Unit
) {
    FitCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                fraction = (progressPercent / 100.0).toFloat(),
                size = 104.dp,
                strokeWidth = 11.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${progressPercent.roundToInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("of goal", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${Formatters.kg(startWeight)} → ${Formatters.kg(targetWeight)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (currentWeight > targetWeight)
                        "${Formatters.kg(currentWeight - targetWeight)} remaining"
                    else "Target reached",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Today's progress: $completionPercent%",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(
    budget: Double,
    spent: Double,
    onClick: () -> Unit
) {
    val snapshot = BudgetCalculator.snapshot(budget, spent)
    FitCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Wallet,
                    contentDescription = null,
                    tint = FitAccent.budget,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Today's budget", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    Formatters.rupees(budget),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            LabeledProgressBar(
                label = if (snapshot.isOverBudget) "Over budget" else "Spent",
                valueText = Formatters.rupees(snapshot.spent),
                fraction = snapshot.usedFraction.toFloat(),
                color = if (snapshot.isOverBudget) MaterialTheme.colorScheme.error else FitAccent.budget
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Text(
                    if (snapshot.isOverBudget)
                        "Over by ${Formatters.rupees(snapshot.overBy)}"
                    else "${Formatters.rupees(snapshot.remaining)} remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (snapshot.isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionBanner(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    FitCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Text(actionLabel) }
        }
    }
}
