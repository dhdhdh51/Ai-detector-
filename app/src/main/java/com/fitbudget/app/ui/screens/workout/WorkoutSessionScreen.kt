package com.fitbudget.app.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.ui.components.ConfirmDialog
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.LabeledProgressBar
import com.fitbudget.app.ui.components.LoadingState
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    templateId: String,
    viewModel: WorkoutSessionViewModel,
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(templateId) { viewModel.load(templateId) }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.template?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.completedCount > 0 || state.skippedCount > 0) {
                            showExitConfirm = true
                        } else {
                            onExit()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val template = state.template
        if (state.loading) {
            LoadingState(modifier = Modifier.padding(padding), label = "Loading workout…")
            return@Scaffold
        }
        if (template == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("This workout is no longer available.", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onExit) { Text("Go back") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FitCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Elapsed", style = MaterialTheme.typography.labelMedium)
                            Text(
                                Formatters.duration(state.elapsedSeconds),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = viewModel::toggleRunning,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                if (state.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.running) "Pause" else "Resume",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LabeledProgressBar(
                        label = "Progress",
                        valueText = "${state.completedCount + state.skippedCount} / ${state.totalCount}",
                        fraction = state.progressFraction,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!state.running && !state.finished) {
                        Spacer(Modifier.height(8.dp))
                        Text("Paused", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            state.currentExercise?.let { exercise ->
                FitCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Exercise ${state.currentIndex + 1} of ${state.totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = FitAccent.workout,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${exercise.sets} sets × ${exercise.repsLabel} · rest ${exercise.restSeconds}s",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(exercise.instructions, style = MaterialTheme.typography.bodyMedium)

                        if (exercise.isTimed) {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Formatters.duration(state.exerciseTimerSeconds ?: 0),
                                        style = MaterialTheme.typography.displaySmall
                                    )
                                    Text(
                                        if ((state.exerciseTimerSeconds ?: 0) == 0) "Time's up"
                                        else "remaining in this set",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = viewModel::resetExerciseTimer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Reset timer")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = viewModel::completeCurrent,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Done")
                            }
                            OutlinedButton(
                                onClick = viewModel::skipCurrent,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Skip")
                            }
                        }
                    }
                }
            }

            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("All exercises", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    template.exercises.forEachIndexed { index, exercise ->
                        val status = state.statuses.getOrNull(index) ?: ExerciseStatus.PENDING
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (status) {
                                    ExerciseStatus.DONE -> "✓"
                                    ExerciseStatus.SKIPPED -> "–"
                                    ExerciseStatus.PENDING -> "○"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = when (status) {
                                    ExerciseStatus.DONE -> MaterialTheme.colorScheme.primary
                                    ExerciseStatus.SKIPPED -> MaterialTheme.colorScheme.error
                                    ExerciseStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    exercise.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index == state.currentIndex) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                                Text(
                                    "${exercise.sets} × ${exercise.repsLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (status == ExerciseStatus.PENDING && index != state.currentIndex) {
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.selectExercise(index) }
                                ) { Text("Go") }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.finish(onExit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = state.completedCount > 0 || state.skippedCount > 0
            ) {
                Text(if (state.allHandled) "Finish workout" else "Finish early & save")
            }
        }
    }

    if (showExitConfirm) {
        ConfirmDialog(
            title = "Leave this workout?",
            message = "Your progress in this session will not be saved unless you finish it.",
            confirmLabel = "Leave",
            destructive = true,
            onConfirm = {
                showExitConfirm = false
                onExit()
            },
            onDismiss = { showExitConfirm = false }
        )
    }
}
