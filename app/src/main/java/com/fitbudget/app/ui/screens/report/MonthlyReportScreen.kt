package com.fitbudget.app.ui.screens.report

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.ui.components.BarChart
import com.fitbudget.app.ui.components.DisclaimerCard
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.LineChart
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.components.StreakChip
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters
import com.fitbudget.app.util.ShareUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: MonthlyReportViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shareFile by viewModel.pendingShareFile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(shareFile) {
        shareFile?.let { file ->
            ShareUtils.shareFile(context, file, "text/plain", "FitBudget monthly report")
            viewModel.consumeShare()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val report = state.report

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
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        DateTimeUtils.formatMonth(state.month),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = viewModel::nextMonth, enabled = state.canGoForward) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            if (report == null || !report.hasData) {
                item {
                    FitCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Nothing tracked this month", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Log meals, water, steps, workouts or a weighing and this report fills in " +
                                    "automatically. FitBudget never invents data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    FitCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Summary", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            MetricRow("Days tracked", "${report.daysWithData} of ${report.daysInMonth}")
                            report.averageWeightKg?.let {
                                MetricRow("Average weight", Formatters.kg(it))
                            }
                            report.weightChangeKg?.let {
                                MetricRow("Weight change", Formatters.signedKg(it))
                            }
                            report.averageWeeklyChangeKg?.let {
                                MetricRow("Average weekly change", "%+.2f kg".format(it))
                            }
                            MetricRow("Average calories eaten", "${report.averageCalories.roundToInt()} kcal")
                            MetricRow("Average food spend", Formatters.rupees(report.averageFoodCost))
                            MetricRow("Budget adherence", "${report.budget.adherencePercent}%")
                            MetricRow("Workouts finished", report.workoutCount.toString())
                            MetricRow("Average steps", Formatters.steps(report.averageSteps))
                            MetricRow("Average water", Formatters.litres(report.averageWaterMl))
                            MetricRow("Water target met", "${report.waterAdherencePercent}% of days")
                            MetricRow("Average checklist", "${report.averageCompletionPercent}%")
                            MetricRow("Meals ticked off", report.mealsCompleted.toString())
                        }
                    }
                }

                if (report.weightSeries.isNotEmpty()) {
                    item {
                        FitCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader("Weight trend")
                                Spacer(Modifier.height(12.dp))
                                LineChart(
                                    points = report.weightSeries,
                                    lineColor = FitAccent.weight,
                                    targetValue = report.targetWeightKg,
                                    startLabel = report.weightSeries.firstOrNull()?.let {
                                        DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                    },
                                    endLabel = report.weightSeries.lastOrNull()?.let {
                                        DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    FitCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader(
                                "Spending trend",
                                subtitle = "${Formatters.rupees(report.budget.totalSpent)} spent of " +
                                    Formatters.rupees(report.budget.totalBudget)
                            )
                            Spacer(Modifier.height(12.dp))
                            BarChart(
                                bars = report.spendSeries,
                                barColor = FitAccent.budget,
                                limitValue = report.budget.totalBudget
                                    .takeIf { report.budget.daysTracked > 0 }
                                    ?.div(report.budget.daysTracked),
                                startLabel = report.spendSeries.firstOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                },
                                endLabel = report.spendSeries.lastOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                },
                                valueFormatter = { "₹%.0f".format(it) }
                            )
                            Spacer(Modifier.height(10.dp))
                            MetricRow("Days under budget", report.budget.daysUnderBudget.toString())
                            MetricRow("Days over budget", report.budget.daysOverBudget.toString())
                        }
                    }
                }

                item {
                    FitCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader("Activity trend")
                            Spacer(Modifier.height(12.dp))
                            BarChart(
                                bars = report.stepSeries.map { it.first to it.second.toDouble() },
                                barColor = FitAccent.steps,
                                startLabel = report.stepSeries.firstOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                },
                                endLabel = report.stepSeries.lastOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                },
                                valueFormatter = { "%.0f".format(it) }
                            )
                        }
                    }
                }

                item {
                    FitCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader("Streaks at the end of this period")
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StreakChip("Diet", report.streaks.diet, modifier = Modifier.weight(1f))
                                StreakChip("Workout", report.streaks.workout, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StreakChip("Water", report.streaks.water, modifier = Modifier.weight(1f))
                                StreakChip("Budget", report.streaks.budget, modifier = Modifier.weight(1f))
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
                            onClick = viewModel::shareReport,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.height(0.dp))
                            Text("  Share file")
                        }
                        OutlinedButton(
                            onClick = {
                                ShareUtils.shareText(
                                    context,
                                    viewModel.reportText(),
                                    "FitBudget monthly report"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Share as text") }
                    }
                }
            }

            item { DisclaimerCard() }
        }
    }
}
