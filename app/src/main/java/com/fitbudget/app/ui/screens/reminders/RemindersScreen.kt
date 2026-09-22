package com.fitbudget.app.ui.screens.reminders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.ui.components.DayOfWeekSelector
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.FitTimePickerDialog
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.util.DateTimeUtils

private val intervalOptions = listOf(60, 90, 120, 180, 240)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedType by remember { mutableStateOf<String?>(null) }
    var timePickerFor by remember { mutableStateOf<ReminderRow?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::rescheduleAll) { Text("Re-sync") }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FitCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("All reminders", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Reminders are scheduled on this device only. They survive restarts and reboots.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = state.masterEnabled,
                            onCheckedChange = viewModel::setMasterEnabled
                        )
                    }
                }
            }

            if (!state.exactAlarmsAllowed) {
                item {
                    FitCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Exact alarms are not permitted for FitBudget, so reminders may be delivered a " +
                                "few minutes late. You can allow them in Android settings → Apps → " +
                                "FitBudget → Alarms & reminders.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    "Schedule",
                    subtitle = "Water reminders repeat between ${DateTimeUtils.formatMinutesOfDay(state.wakeMinutes)} " +
                        "and ${DateTimeUtils.formatMinutesOfDay(state.sleepMinutes)} (your wake and sleep times)."
                )
            }

            items(state.rows, key = { it.type.name }) { row ->
                val expanded = expandedType == row.type.name
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.type.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (row.reminder.type.isInterval) {
                                        "From ${DateTimeUtils.formatTime(row.reminder.hour, row.reminder.minute)} · " +
                                            "every ${row.reminder.intervalMinutes / 60}h" +
                                            (if (row.reminder.intervalMinutes % 60 != 0) " ${row.reminder.intervalMinutes % 60}m" else "")
                                    } else {
                                        DateTimeUtils.formatTime(row.reminder.hour, row.reminder.minute) +
                                            " · " + DateTimeUtils.describeMask(row.reminder.daysMask)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    row.nextTriggerLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (row.nextTrigger != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = row.reminder.enabled && state.masterEnabled,
                                enabled = state.masterEnabled,
                                onCheckedChange = { viewModel.setEnabled(row.type, it) }
                            )
                        }

                        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                            TextButton(onClick = { timePickerFor = row }) { Text("Change time") }
                            TextButton(
                                onClick = {
                                    expandedType = if (expanded) null else row.type.name
                                }
                            ) { Text(if (expanded) "Hide options" else "More options") }
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                if (!row.type.isInterval) {
                                    Text("Repeat on", style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.height(6.dp))
                                    DayOfWeekSelector(
                                        daysMask = row.reminder.daysMask,
                                        onMaskChange = { viewModel.setDays(row.type, it) }
                                    )
                                } else {
                                    Text("Repeat every", style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        intervalOptions.forEach { minutes ->
                                            FilterChip(
                                                selected = row.reminder.intervalMinutes == minutes,
                                                onClick = { viewModel.setInterval(row.type, minutes) },
                                                label = {
                                                    Text(
                                                        if (minutes % 60 == 0) "${minutes / 60}h"
                                                        else "${minutes}m"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Sound",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = row.reminder.soundEnabled,
                                        onCheckedChange = { viewModel.setSound(row.type, it) }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Vibration",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = row.reminder.vibrationEnabled,
                                        onCheckedChange = { viewModel.setVibration(row.type, it) }
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Notification: \"${row.type.notificationTitle} — ${row.type.notificationMessage}\"",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Tapping it opens the ${row.type.route} screen.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    timePickerFor?.let { row ->
        FitTimePickerDialog(
            initialHour = row.reminder.hour,
            initialMinute = row.reminder.minute,
            onDismiss = { timePickerFor = null },
            onConfirm = { hour, minute ->
                viewModel.setTime(row.type, hour, minute)
                timePickerFor = null
            },
            title = "${row.type.label} reminder"
        )
    }
}
