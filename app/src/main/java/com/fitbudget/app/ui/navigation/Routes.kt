package com.fitbudget.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ONBOARDING = "onboarding"
    const val PLAN_READY = "plan_ready"

    const val HOME = "home"
    const val DIET = "diet"
    const val WORKOUT = "workout"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    const val WATER = "water"
    const val WEIGHT = "weight"
    const val STEPS = "steps"
    const val BUDGET = "budget"
    const val MONTHLY_REPORT = "monthly_report"
    const val SETTINGS = "settings"
    const val REMINDERS = "reminders"
    const val FOODS = "foods"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"

    const val WORKOUT_SESSION_ARG = "templateId"
    const val WORKOUT_SESSION_PATTERN = "workout_session/{$WORKOUT_SESSION_ARG}"

    fun workoutSession(templateId: String) = "workout_session/$templateId"

    /** Routes a notification is allowed to deep-link into. */
    val deepLinkable = setOf(HOME, DIET, WORKOUT, PROGRESS, PROFILE, WATER, WEIGHT, STEPS, BUDGET)
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.DIET, "Diet", Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu),
    BottomNavItem(Routes.WORKOUT, "Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    BottomNavItem(Routes.PROGRESS, "Progress", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)
