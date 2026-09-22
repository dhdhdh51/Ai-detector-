package com.fitbudget.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitbudget.app.data.seed.WorkoutTemplate
import com.fitbudget.app.domain.model.WorkoutCategory
import com.fitbudget.app.ui.components.EmptyState
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.SectionHeader
import com.fitbudget.app.ui.theme.FitAccent
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onStartWorkout: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Workouts", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (state.todayCount > 0)
                        "${state.todayCount} session${if (state.todayCount == 1) "" else "s"} done today"
                    else "No session logged today yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WorkoutCategory.entries) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.label) }
                    )
                }
            }
        }

        items(state.templates, key = { it.id }) { template ->
            TemplateCard(template = template, onStart = { onStartWorkout(template.id) })
        }

        item {
            SectionHeader(
                "History",
                subtitle = if (state.history.isEmpty()) null
                else "${state.totalSessions} sessions · ${state.totalMinutes} minutes total",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (state.history.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.FitnessCenter,
                    title = "No workouts finished yet",
                    message = "Finish your first session and it will appear here."
                )
            }
        }

        items(state.history, key = { it.id }) { session ->
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(session.templateName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${DateTimeUtils.relativeDayLabel(session.epochDay)} · " +
                                "${Formatters.duration(session.durationSeconds)} · " +
                                "${session.exercisesCompleted}/${session.exercisesTotal} exercises",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "≈ ${session.estimatedCalories} kcal (estimate)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteSession(session) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete session")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TemplateCard(template: WorkoutTemplate, onStart: () -> Unit) {
    FitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        template.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = FitAccent.workout,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(onClick = onStart, shape = RoundedCornerShape(14.dp)) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Start")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${template.exercises.size} exercises · ${template.totalSets} sets · " +
                    "${template.estimatedMinutes} min · ≈ ${template.estimatedCalories} kcal",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "Equipment: ${template.equipment}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            template.exercises.take(3).forEach { exercise ->
                Text(
                    "• ${exercise.name} — ${exercise.sets} × ${exercise.repsLabel}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (template.exercises.size > 3) {
                Text(
                    "• +${template.exercises.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
