package com.fitbudget.app.ui.screens.steps

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.DisposableEffect
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
import com.fitbudget.app.domain.model.StepSource
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.ui.components.MiniBarRow
import com.fitbudget.app.ui.components.NumberField
import com.fitbudget.app.ui.components.ProgressRing
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsScreen(
    viewModel: StepsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showGoalDialog by remember { mutableStateOf(false) }
    var manualValue by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startSensor()
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    DisposableEffect(Unit) {
        viewModel.startSensor()
        onDispose { viewModel.stopSensor() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showGoalDialog = true }) { Text("Goal") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProgressRing(
                            fraction = state.fraction,
                            size = 170.dp,
                            strokeWidth = 16.dp,
                            color = FitAccent.steps
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    Formatters.steps(state.totalSteps),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "of ${Formatters.steps(state.goal)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            if (state.goalMet) "Goal reached today."
                            else "${Formatters.steps(state.remaining)} steps to go",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!state.sensorAvailable) {
                item {
                    FitCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No step sensor on this device", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Enter your steps manually below — everything else keeps working exactly the same.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else if (!state.sensorPermissionGranted) {
                item {
                    FitCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Automatic counting is off", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Allow physical activity access and FitBudget will read your device's step counter.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Allow") }
                        }
                    }
                }
            }

            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            "Manual entry",
                            subtitle = "Adds to whatever the sensor already counted."
                        )
                        Spacer(Modifier.height(12.dp))
                        NumberField(
                            label = "Manual steps for today",
                            value = manualValue.ifEmpty { state.manualSteps.toString() },
                            onValueChange = { manualValue = it },
                            decimal = false,
                            supportingText = "0 – 100,000"
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.setManualSteps(
                                        manualValue.ifEmpty { state.manualSteps.toString() }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Save") }
                            OutlinedButton(
                                onClick = { viewModel.addManualSteps(1_000) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("+1,000") }
                        }
                    }
                }
            }

            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(
                            "Last 7 days",
                            subtitle = "Average ${Formatters.steps(state.averageWeek)} steps"
                        )
                        Spacer(Modifier.height(12.dp))
                        MiniBarRow(
                            values = state.weekTotals,
                            goal = state.goal,
                            barColor = FitAccent.steps
                        )
                    }
                }
            }

            item {
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        MetricRow("Counted by sensor", Formatters.steps(state.sensorSteps))
                        MetricRow("Entered manually", Formatters.steps(state.manualSteps))
                        MetricRow("Daily goal", Formatters.steps(state.goal))
                        MetricRow(
                            "Source",
                            when (state.source) {
                                StepSource.SENSOR -> "Device sensor"
                                StepSource.MANUAL -> "Manual entry"
                                StepSource.MIXED -> "Sensor + manual"
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The device counts steps in the background; FitBudget reads the total each time you open it.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        var goalValue by remember { mutableStateOf(state.goal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Daily step goal") },
            text = {
                NumberField(
                    label = "Step goal",
                    value = goalValue,
                    onValueChange = { goalValue = it },
                    decimal = false,
                    supportingText = "8,000 is a solid target for fat loss."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setGoal(goalValue)
                    showGoalDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }
}
