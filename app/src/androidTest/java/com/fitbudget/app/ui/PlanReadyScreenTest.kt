package com.fitbudget.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitbudget.app.ui.screens.onboarding.PlanReadyScreen
import com.fitbudget.app.ui.screens.onboarding.PlanSummary
import com.fitbudget.app.ui.theme.FitBudgetTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for the end of the first-launch flow: the summary the brief requires
 * ("Your plan is ready." + current weight, target, budget, water goal, step goal).
 */
@RunWith(AndroidJUnit4::class)
class PlanReadyScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val summary = PlanSummary(
        name = "Rahul",
        currentWeightKg = 85.0,
        targetWeightKg = 75.0,
        dailyBudget = 100.0,
        waterTargetMl = 3000,
        stepGoal = 8000,
        calorieTarget = 2020.0,
        proteinTargetG = 120.0,
        bmi = 28.7,
        bmiCategory = "Overweight",
        weeklyTrendKg = 0.46
    )

    @Test
    fun summaryShowsEveryRequiredValue() {
        composeRule.setContent {
            FitBudgetTheme { PlanReadyScreen(summary = summary, onStart = {}) }
        }

        composeRule.onNodeWithText("Your plan is ready.").assertIsDisplayed()
        composeRule.onNodeWithText("Let's get started, Rahul.").assertIsDisplayed()
        composeRule.onNodeWithText("85.0 kg").assertIsDisplayed()
        composeRule.onNodeWithText("75.0 kg").assertIsDisplayed()
        composeRule.onNodeWithText("₹100").assertIsDisplayed()
        composeRule.onNodeWithText("3.00 L").assertIsDisplayed()
        composeRule.onNodeWithText("8,000").assertIsDisplayed()
    }

    @Test
    fun disclaimerIsAlwaysVisibleOnTheSummary() {
        composeRule.setContent {
            FitBudgetTheme { PlanReadyScreen(summary = summary, onStart = {}) }
        }
        composeRule.onNodeWithText(
            "Nutrition estimates are approximate",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun startButtonReportsTheTap() {
        var started = false
        composeRule.setContent {
            FitBudgetTheme { PlanReadyScreen(summary = summary, onStart = { started = true }) }
        }

        composeRule.onNodeWithText("Open my dashboard").performClick()
        assertTrue("tapping the dashboard button should notify the caller", started)
    }
}
