package com.fitbudget.app.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitbudget.app.BuildConfig
import com.fitbudget.app.R
import com.fitbudget.app.ui.components.DisclaimerCard
import com.fitbudget.app.ui.components.FitCard
import com.fitbudget.app.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    InfoScaffold(title = "Privacy", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FitCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Your data never leaves this device.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "FitBudget has no account, no server and no analytics. Everything you log is " +
                                "stored in a local database on this phone.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item { SectionHeader("What is stored locally") }
            item {
                InfoCard(
                    listOf(
                        "Your profile: name, date of birth, gender, height, weights, budget, activity level, food preference and wake/sleep times.",
                        "Your logs: weigh-ins, meals and their cost, extra expenses, water, steps, workout sessions.",
                        "Your settings: theme, units, goals, reminder times and food exclusions."
                    )
                )
            }

            item { SectionHeader("What FitBudget never does") }
            item {
                InfoCard(
                    listOf(
                        "No account creation, sign-in or email address.",
                        "No upload of health, fitness or spending information to any server.",
                        "No analytics, tracking SDKs, advertising identifiers or crash reporting services.",
                        "No use of the internet for any core feature - the app works fully offline."
                    )
                )
            }

            item { SectionHeader("Permissions and why they are needed") }
            item {
                InfoCard(
                    listOf(
                        "Notifications: to show your meal, water, workout, weight and sleep reminders.",
                        "Alarms & reminders (exact alarms): so reminders arrive at the time you chose.",
                        "Physical activity: only to read the step counter your phone already maintains. Optional - manual entry works instead.",
                        "Run at startup: to rebuild your reminder schedule after a reboot.",
                        "Vibration: for reminder vibration, if you keep it switched on."
                    )
                )
            }

            item { SectionHeader("Sharing and deletion") }
            item {
                InfoCard(
                    listOf(
                        "Export and backup files are written to this app's private storage and only leave the device if you pick an app in the share sheet yourself.",
                        "\"Reset all data\" in Settings permanently erases everything from this device.",
                        "Uninstalling FitBudget removes the local database and all settings."
                    )
                )
            }

            item { DisclaimerCard() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    InfoScaffold(title = "About", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_splash_logo),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "FitBudget",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SectionHeader("What this app is") }
            item {
                InfoCard(
                    listOf(
                        "A budget-first fat-loss companion built around an affordable Indian diet.",
                        "It plans meals from a bundled food database, tracks what you actually eat and spend, and keeps your water, steps, workouts and weight in one place.",
                        "Everything works offline. There is no account and no server."
                    )
                )
            }

            item { SectionHeader("How the estimates work") }
            item {
                InfoCard(
                    listOf(
                        "BMI = weight (kg) ÷ height (m)².",
                        "Maintenance calories use the Mifflin-St Jeor equation multiplied by your activity level.",
                        "The daily target applies a moderate deficit to maintenance, never below a conservative floor.",
                        "Weekly weight trend assumes roughly 7,700 kcal per kilogram of body fat.",
                        "Progress = (start weight − current weight) ÷ (start weight − target weight) × 100."
                    )
                )
            }

            item { SectionHeader("Important") }
            item {
                FitCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "FitBudget does not diagnose, treat or promise any outcome. Nutrition and calorie " +
                            "figures are approximate estimates. For medical conditions, pregnancy, or " +
                            "special dietary needs, consult a qualified healthcare professional.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { SectionHeader("Credits") }
            item {
                InfoCard(
                    listOf(
                        "Built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, WorkManager and AlarmManager.",
                        "All icons and the FitBudget brand mark are drawn in-project; no third-party or copyrighted assets are used."
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        content(
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            )
        )
    }
}

@Composable
private fun InfoCard(lines: List<String>) {
    FitCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            lines.forEachIndexed { index, line ->
                Text(
                    "•  $line",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (index != lines.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}
