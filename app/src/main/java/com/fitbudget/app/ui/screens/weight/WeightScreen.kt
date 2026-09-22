package com.fitbudget.app.ui.screens.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MonitorWeight
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.data.database.entity.WeightLogEntity
import com.fitbudget.app.ui.components.ConfirmDialog
import com.fitbudget.app.ui.components.EmptyState
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.LineChart
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.PlainTextField
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    viewModel: WeightViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<WeightLogEntity?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight tracker") },
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
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            "Log a weighing",
                            subtitle = DateTimeUtils.relativeDayLabel(form.epochDay) + " · " +
                                DateTimeUtils.formatFullDate(DateTimeUtils.date(form.epochDay))
                        )
                        Spacer(Modifier.height(12.dp))
                        NumberField(
                            label = "Weight",
                            value = form.weight,
                            onValueChange = viewModel::onWeightChange,
                            suffix = "kg",
                            errorMessage = form.error,
                            supportingText = "Weigh yourself at the same time each day for a clean trend."
                        )
                        Spacer(Modifier.height(4.dp))
                        PlainTextField(
                            label = "Note (optional)",
                            value = form.note,
                            onValueChange = viewModel::onNoteChange
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.onDateChange(form.epochDay - 1) }
                            ) { Text("Previous day") }
                            if (form.epochDay < DateTimeUtils.todayEpochDay()) {
                                TextButton(
                                    onClick = { viewModel.onDateChange(form.epochDay + 1) }
                                ) { Text("Next day") }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.save() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !form.saving,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Save weight") }
                    }
                }
            }

            if (state.logs.size >= 1) {
                item {
                    FitCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader("All time trend")
                            Spacer(Modifier.height(12.dp))
                            LineChart(
                                points = state.chartPoints,
                                lineColor = FitAccent.weight,
                                targetValue = state.profile?.targetWeightKg,
                                startLabel = state.chartPoints.firstOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                },
                                endLabel = state.chartPoints.lastOrNull()?.let {
                                    DateTimeUtils.formatShortDate(DateTimeUtils.date(it.first))
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                            MetricRow(
                                "Starting weight",
                                Formatters.kg(state.profile?.startWeightKg ?: 0.0)
                            )
                            MetricRow(
                                "Current weight",
                                Formatters.kg(state.latest?.weightKg ?: 0.0)
                            )
                            MetricRow(
                                "Target weight",
                                Formatters.kg(state.profile?.targetWeightKg ?: 0.0)
                            )
                            state.totalChangeKg?.let {
                                MetricRow("Change since first entry", Formatters.signedKg(it))
                            }
                            state.averageWeeklyChangeKg?.let {
                                MetricRow("Average weekly change", "%+.2f kg".format(it))
                            }
                        }
                    }
                }
            }

            item { SectionHeader("History", subtitle = "Newest first") }

            if (state.logs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.MonitorWeight,
                        title = "No weight entries yet",
                        message = "Save your first weighing above and the chart will appear."
                    )
                }
            }

            items(state.logs, key = { it.id }) { log ->
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Formatters.kg(log.weightKg),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                DateTimeUtils.formatFullDate(DateTimeUtils.date(log.epochDay)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            log.note?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { pendingDelete = log }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete entry")
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { log ->
        ConfirmDialog(
            title = "Delete this entry?",
            message = "${Formatters.kg(log.weightKg)} on " +
                "${DateTimeUtils.formatFullDate(DateTimeUtils.date(log.epochDay))} will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.delete(log)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
