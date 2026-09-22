package com.fitbudget.app.ui.screens.budget

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.fitbudget.app.ui.components.BarChart
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.LabeledProgressBar
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.PlainTextField
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.components.StreakChip
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food budget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showBudgetDialog = true }) { Text("Edit") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val snapshot = state.snapshot

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FitCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (snapshot.isOverBudget)
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            DateTimeUtils.relativeDayLabel(state.epochDay) + "'s budget",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            Formatters.rupees(state.budget),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
                        LabeledProgressBar(
                            label = "Spent",
                            valueText = Formatters.rupees(snapshot.spent),
                            fraction = snapshot.usedFraction.toFloat(),
                            color = if (snapshot.isOverBudget) MaterialTheme.colorScheme.error
                            else FitAccent.budget
                        )
                        Spacer(Modifier.height(12.dp))
                        MetricRow(
                            if (snapshot.isOverBudget) "Over budget by" else "Remaining",
                            if (snapshot.isOverBudget) Formatters.rupees(snapshot.overBy)
                            else Formatters.rupees(snapshot.remaining),
                            valueColor = if (snapshot.isOverBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                        MetricRow("Planned meal cost today", Formatters.rupees(state.plannedCost))
                        if (snapshot.pendingPlannedCost > 0) {
                            MetricRow(
                                "Still to eat (planned)",
                                Formatters.rupees(snapshot.pendingPlannedCost)
                            )
                            if (snapshot.projectedOverBudget) {
                                Text(
                                    "Finishing the whole plan would put you over budget.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Spending counts a meal once you tick it off in the Diet tab, plus any extras you add here.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { showExpenseDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Add an extra food expense") }
            }

            if (state.expenses.isNotEmpty()) {
                item { SectionHeader("Extra expenses today") }
                items(state.expenses, key = { it.id }) { expense ->
                    FitCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                expense.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                Formatters.rupees(expense.amount),
                                style = MaterialTheme.typography.titleSmall
                            )
                            IconButton(onClick = { viewModel.deleteExpense(expense) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove expense")
                            }
                        }
                    }
                }
            }

            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = viewModel::showPreviousMonth) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                            }
                            Text(
                                DateTimeUtils.formatMonth(state.month),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = viewModel::showNextMonth) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (state.monthly.daysTracked > 0) {
                            BarChart(
                                bars = state.spendSeries,
                                barColor = FitAccent.budget,
                                limitValue = state.budget,
                                startLabel = state.monthSummaries.firstOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.epochDay))
                                },
                                endLabel = state.monthSummaries.lastOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.epochDay))
                                },
                                valueFormatter = { "₹%.0f".format(it) }
                            )
                            Spacer(Modifier.height(14.dp))
                            MetricRow("Days tracked", state.monthly.daysTracked.toString())
                            MetricRow("Total budget", Formatters.rupees(state.monthly.totalBudget))
                            MetricRow("Total spent", Formatters.rupees(state.monthly.totalSpent))
                            MetricRow(
                                "Average a day",
                                Formatters.rupees(state.monthly.averageDailySpend)
                            )
                            MetricRow("Days under budget", state.monthly.daysUnderBudget.toString())
                            MetricRow("Days over budget", state.monthly.daysOverBudget.toString())
                            MetricRow("Budget adherence", "${state.monthly.adherencePercent}%")
                        } else {
                            Text(
                                "No spending tracked in this month yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        StreakChip("Budget streak", state.budgetStreak)
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        var value by remember { mutableStateOf(state.budget.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Daily food budget") },
            text = {
                NumberField(
                    label = "Budget",
                    value = value,
                    onValueChange = { value = it },
                    suffix = "₹",
                    decimal = false,
                    supportingText = "Between ₹1 and ₹10,000. Past days keep their old budget."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDailyBudget(value)
                    showBudgetDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showExpenseDialog) {
        var label by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false },
            title = { Text("Extra food expense") },
            text = {
                Column {
                    PlainTextField(
                        label = "What was it?",
                        value = label,
                        onValueChange = { label = it },
                        supportingText = "e.g. tea at the stall"
                    )
                    Spacer(Modifier.height(8.dp))
                    NumberField(
                        label = "Amount",
                        value = amount,
                        onValueChange = { amount = it },
                        suffix = "₹"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addExpense(label, amount)
                    showExpenseDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseDialog = false }) { Text("Cancel") }
            }
        )
    }
}
