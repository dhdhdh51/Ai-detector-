package com.fitbudget.app.ui.screens.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Undo
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
import com.fitbudget.app.domain.WaterCalculator
import com.fitbudget.app.ui.components.ConfirmDialog
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.ui.components.MiniBarRow
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.ProgressRing
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.components.StreakChip
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    viewModel: WaterViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showTargetDialog = true }) { Text("Target") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val snapshot = state.snapshot

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
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProgressRing(
                            fraction = snapshot.fraction.toFloat(),
                            size = 170.dp,
                            strokeWidth = 16.dp,
                            color = FitAccent.water
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    Formatters.litres(state.consumedMl),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "of ${Formatters.litres(state.targetMl)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${snapshot.percent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = FitAccent.water
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (snapshot.isGoalMet) "Target reached — nicely done."
                            else "${Formatters.ml(snapshot.remainingMl)} to go · about ${snapshot.glassesRemaining} glasses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WaterCalculator.QUICK_AMOUNTS_ML.forEach { amount ->
                        Button(
                            onClick = { viewModel.add(amount) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("+$amount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::undoLast,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Undo last")
                    }
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Clear today") }
                }
            }

            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            "Last 7 days",
                            subtitle = "Average ${Formatters.litres(state.averageWeekMl)} a day"
                        )
                        Spacer(Modifier.height(12.dp))
                        MiniBarRow(
                            values = state.weekTotals,
                            goal = state.targetMl,
                            barColor = FitAccent.water
                        )
                        Spacer(Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            StreakChip("Water streak", state.streakDays)
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    "Today's entries",
                    subtitle = DateTimeUtils.formatFullDate(DateTimeUtils.date(state.epochDay))
                )
            }

            if (state.logs.isEmpty()) {
                item {
                    Text(
                        "Nothing logged yet today. Use the buttons above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            items(state.logs, key = { it.id }) { log ->
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Formatters.ml(log.amountMl),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            DateTimeUtils.localDateTimeOf(log.loggedAtMillis).let {
                                DateTimeUtils.formatTime(it.hour, it.minute)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { viewModel.delete(log) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove entry")
                        }
                    }
                }
            }

            item {
                MetricRow("Daily target", Formatters.litres(state.targetMl))
            }
        }
    }

    if (showTargetDialog) {
        var value by remember { mutableStateOf(state.targetMl.toString()) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("Daily water target") },
            text = {
                Column {
                    NumberField(
                        label = "Target",
                        value = value,
                        onValueChange = { value = it },
                        suffix = "ml",
                        decimal = false,
                        supportingText = "Between 500 ml and 10,000 ml. 2,500–3,000 ml suits most people."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTarget(value)
                    showTargetDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Clear today's water log?",
            message = "All of today's entries will be removed. History for other days is kept.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = {
                viewModel.clearToday()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}
