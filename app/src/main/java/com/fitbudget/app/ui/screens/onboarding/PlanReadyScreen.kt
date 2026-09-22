package com.fitbudget.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitbudget.app.ui.components.DisclaimerCard
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.MetricRow
import com.fitbudget.app.util.Formatters

/** Shown once, right after onboarding: "Your plan is ready." */
@Composable
fun PlanReadyScreen(
    summary: PlanSummary?,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Your plan is ready.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            summary?.name?.takeIf { it.isNotBlank() }
                ?.let { "Let's get started, $it." }
                ?: "Let's get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        if (summary != null) {
            FitCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    MetricRow("Current weight", Formatters.kg(summary.currentWeightKg))
                    MetricRow("Target weight", Formatters.kg(summary.targetWeightKg))
                    MetricRow("Daily food budget", Formatters.rupees(summary.dailyBudget))
                    MetricRow("Water goal", Formatters.litres(summary.waterTargetMl))
                    MetricRow("Step goal", Formatters.steps(summary.stepGoal))
                }
            }
            Spacer(Modifier.height(12.dp))
            FitCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Estimates", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    MetricRow("Daily calories", Formatters.calories(summary.calorieTarget))
                    MetricRow("Protein target", Formatters.grams(summary.proteinTargetG))
                    MetricRow("BMI", "%.1f · %s".format(summary.bmi, summary.bmiCategory))
                    MetricRow(
                        "Estimated trend",
                        "%.2f kg / week".format(summary.weeklyTrendKg)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            DisclaimerCard()
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Open my dashboard")
        }
        Spacer(Modifier.height(24.dp))
    }
}
