package com.fitbudget.app.ui.screens.progress

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
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.ui.components.BarChart
import com.fitbudget.app.ui.components.EmptyState
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.LineChart
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.components.StreakChip
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onLogWeight: () -> Unit,
    onOpenMonthlyReport: () -> Unit,
    contentPadding: PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Progress", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Everything here comes from what you have logged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        "Weight trend",
                        subtitle = "${state.weightLogCount} entr" +
                            (if (state.weightLogCount == 1) "y" else "ies") + " logged"
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.hasWeightData) {
                        LineChart(
                            points = state.weightPoints,
                            lineColor = FitAccent.weight,
                            targetValue = profile?.targetWeightKg,
                            startLabel = state.weightPoints.firstOrNull()
                                ?.let { viewModel.label(it.first) },
                            endLabel = state.weightPoints.lastOrNull()
                                ?.let { viewModel.label(it.first) },
                            valueFormatter = { "%.1f".format(it) }
                        )
                        Spacer(Modifier.height(12.dp))
                        MetricRow("Starting weight", Formatters.kg(profile?.startWeightKg ?: 0.0))
                        MetricRow("Current weight", Formatters.kg(profile?.currentWeightKg ?: 0.0))
                        MetricRow("Target weight", Formatters.kg(profile?.targetWeightKg ?: 0.0))
                        MetricRow(
                            "Total change",
                            Formatters.signedKg(-state.totalLostKg),
                            valueColor = if (state.totalLostKg > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        MetricRow(
                            "Average weekly change",
                            if (state.weightPoints.size > 1)
                                "%+.2f kg".format(state.averageWeeklyChangeKg)
                            else "Needs 2+ entries"
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The dashed line is your target weight.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.MonitorWeight,
                            title = "No weight entries yet",
                            message = "Log today's weight to start your trend chart.",
                            actionLabel = "Log weight",
                            onAction = onLogWeight
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLogWeight,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Log weight") }
                OutlinedButton(
                    onClick = onOpenMonthlyReport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Monthly report") }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        "Food spending",
                        subtitle = "Average ${Formatters.rupees(state.averageSpend)} a day"
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.summaries.any { it.spent > 0 }) {
                        BarChart(
                            bars = state.spendSeries,
                            barColor = FitAccent.budget,
                            limitValue = profile?.dailyBudget,
                            startLabel = state.summaries.firstOrNull()
                                ?.let { viewModel.label(it.epochDay) },
                            endLabel = state.summaries.lastOrNull()
                                ?.let { viewModel.label(it.epochDay) },
                            valueFormatter = { "₹%.0f".format(it) }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The dashed line is your daily budget. Red bars went over.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Tick meals off in the Diet tab and your spending will show up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        "Activity",
                        subtitle = "Average ${Formatters.steps(state.averageSteps)} steps · " +
                            "${state.workoutsInRange} workouts"
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.summaries.any { it.steps > 0 }) {
                        BarChart(
                            bars = state.stepSeries,
                            barColor = FitAccent.steps,
                            limitValue = state.summaries.lastOrNull()?.stepGoal?.toDouble(),
                            startLabel = state.summaries.firstOrNull()
                                ?.let { viewModel.label(it.epochDay) },
                            endLabel = state.summaries.lastOrNull()
                                ?.let { viewModel.label(it.epochDay) },
                            valueFormatter = { "%.0f".format(it) }
                        )
                    } else {
                        Text(
                            "No steps recorded for this period yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        "Daily checklist",
                        subtitle = "Average ${state.averageCompletion}% completed"
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.summaries.any { it.hasAnyActivity }) {
                        BarChart(
                            bars = state.completionSeries,
                            barColor = FitAccent.diet,
                            limitValue = 100.0,
                            startLabel = state.summaries.firstOrNull()
                                ?.let { viewModel.label(it.epochDay) },
                            endLabel = state.summaries.lastOrNull()
                                ?.let { viewModel.label(it.epochDay) },
                            valueFormatter = { "%.0f%%".format(it) }
                        )
                    } else {
                        Text(
                            "Start ticking items off and this chart fills in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Current streaks")
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
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Today counts towards a streak only once the goal is actually met.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                "All calculations shown are estimates, not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Suppress("unused")
private fun dayLabel(epochDay: Long): String =
    DateTimeUtils.formatShortDate(DateTimeUtils.date(epochDay))
