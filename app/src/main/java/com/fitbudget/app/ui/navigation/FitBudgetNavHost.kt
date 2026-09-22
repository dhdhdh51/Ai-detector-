package com.fitbudget.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitbudget.app.ui.LocalAppViewModelFactory
import com.fitbudget.app.ui.screens.budget.BudgetScreen
import com.fitbudget.app.ui.screens.budget.BudgetViewModel
import com.fitbudget.app.ui.screens.diet.DietScreen
import com.fitbudget.app.ui.screens.diet.DietViewModel
import com.fitbudget.app.ui.screens.foods.FoodsScreen
import com.fitbudget.app.ui.screens.foods.FoodsViewModel
import com.fitbudget.app.ui.screens.home.HomeScreen
import com.fitbudget.app.ui.screens.home.HomeViewModel
import com.fitbudget.app.ui.screens.info.AboutScreen
import com.fitbudget.app.ui.screens.info.PrivacyScreen
import com.fitbudget.app.ui.screens.onboarding.OnboardingScreen
import com.fitbudget.app.ui.screens.onboarding.OnboardingViewModel
import com.fitbudget.app.ui.screens.onboarding.PlanReadyScreen
import com.fitbudget.app.ui.screens.profile.ProfileScreen
import com.fitbudget.app.ui.screens.profile.ProfileViewModel
import com.fitbudget.app.ui.screens.progress.ProgressScreen
import com.fitbudget.app.ui.screens.progress.ProgressViewModel
import com.fitbudget.app.ui.screens.reminders.RemindersScreen
import com.fitbudget.app.ui.screens.reminders.RemindersViewModel
import com.fitbudget.app.ui.screens.report.MonthlyReportScreen
import com.fitbudget.app.ui.screens.report.MonthlyReportViewModel
import com.fitbudget.app.ui.screens.settings.SettingsScreen
import com.fitbudget.app.ui.screens.settings.SettingsViewModel
import com.fitbudget.app.ui.screens.steps.StepsScreen
import com.fitbudget.app.ui.screens.steps.StepsViewModel
import com.fitbudget.app.ui.screens.water.WaterScreen
import com.fitbudget.app.ui.screens.water.WaterViewModel
import com.fitbudget.app.ui.screens.weight.WeightScreen
import com.fitbudget.app.ui.screens.weight.WeightViewModel
import com.fitbudget.app.ui.screens.workout.WorkoutScreen
import com.fitbudget.app.ui.screens.workout.WorkoutSessionScreen
import com.fitbudget.app.ui.screens.workout.WorkoutSessionViewModel
import com.fitbudget.app.ui.screens.workout.WorkoutViewModel
import kotlinx.coroutines.flow.Flow

/**
 * Single navigation graph for the whole app.
 *
 * * Onboarding is shown until a profile is marked complete.
 * * The five top-level destinations share a bottom bar; detail screens own their top bar.
 * * [pendingDeepLink] carries a route from a tapped notification.
 */
@Composable
fun FitBudgetNavHost(
    onboardingComplete: Boolean,
    pendingDeepLink: String?,
    onDeepLinkHandled: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    LaunchedEffect(onboardingComplete) {
        if (!onboardingComplete && currentRoute != null && currentRoute != Routes.ONBOARDING) {
            navController.navigate(Routes.ONBOARDING) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(pendingDeepLink, onboardingComplete) {
        val route = pendingDeepLink
        if (route != null && onboardingComplete && route in Routes.deepLinkable) {
            navController.navigate(route) {
                launchSingleTop = true
            }
            onDeepLinkHandled()
        } else if (route != null) {
            onDeepLinkHandled()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // Detail screens draw their own top bar, so only the horizontal + bottom insets are shared.
        val listPadding = PaddingValues(
            start = scaffoldPadding.calculateStartPadding(layoutDirection) + 16.dp,
            end = scaffoldPadding.calculateEndPadding(layoutDirection) + 16.dp,
            top = scaffoldPadding.calculateTopPadding() + 8.dp,
            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp
        )

        NavHost(
            navController = navController,
            startDestination = if (onboardingComplete) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.ONBOARDING) {
                val viewModel: OnboardingViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                OnboardingScreen(
                    viewModel = viewModel,
                    onFinished = {
                        navController.navigate(Routes.PLAN_READY) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.PLAN_READY) {
                val viewModel: OnboardingViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                PlanReadyScreen(
                    summary = state.summary,
                    onStart = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                val viewModel: HomeViewModel = viewModel(factory = LocalAppViewModelFactory.current)
                SnackbarMessages(viewModel.messages, snackbarHostState)
                HomeScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    contentPadding = listPadding
                )
            }

            composable(Routes.DIET) {
                val viewModel: DietViewModel = viewModel(factory = LocalAppViewModelFactory.current)
                SnackbarMessages(viewModel.messages, snackbarHostState)
                DietScreen(viewModel = viewModel, contentPadding = listPadding)
            }

            composable(Routes.WORKOUT) {
                val viewModel: WorkoutViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                SnackbarMessages(viewModel.messages, snackbarHostState)
                WorkoutScreen(
                    viewModel = viewModel,
                    onStartWorkout = { id -> navController.navigate(Routes.workoutSession(id)) },
                    contentPadding = listPadding
                )
            }

            composable(Routes.PROGRESS) {
                val viewModel: ProgressViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                SnackbarMessages(viewModel.messages, snackbarHostState)
                ProgressScreen(
                    viewModel = viewModel,
                    onLogWeight = { navController.navigate(Routes.WEIGHT) },
                    onOpenMonthlyReport = { navController.navigate(Routes.MONTHLY_REPORT) },
                    contentPadding = listPadding
                )
            }

            composable(Routes.PROFILE) {
                val viewModel: ProfileViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                SnackbarMessages(viewModel.messages, snackbarHostState)
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    contentPadding = listPadding
                )
            }

            composable(Routes.WATER) {
                val viewModel: WaterViewModel = viewModel(factory = LocalAppViewModelFactory.current)
                WaterScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.WEIGHT) {
                val viewModel: WeightViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                WeightScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.STEPS) {
                val viewModel: StepsViewModel = viewModel(factory = LocalAppViewModelFactory.current)
                StepsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.BUDGET) {
                val viewModel: BudgetViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                BudgetScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.MONTHLY_REPORT) {
                val viewModel: MonthlyReportViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                MonthlyReportScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                    onOpenFoods = { navController.navigate(Routes.FOODS) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onDataReset = {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.REMINDERS) {
                val viewModel: RemindersViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                RemindersScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.FOODS) {
                val viewModel: FoodsViewModel = viewModel(factory = LocalAppViewModelFactory.current)
                FoodsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.WORKOUT_SESSION_PATTERN) { entry ->
                val templateId = entry.arguments?.getString(Routes.WORKOUT_SESSION_ARG).orEmpty()
                val viewModel: WorkoutSessionViewModel =
                    viewModel(factory = LocalAppViewModelFactory.current)
                WorkoutSessionScreen(
                    templateId = templateId,
                    viewModel = viewModel,
                    onExit = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Collects a ViewModel's one-off messages into the shared snackbar host. */
@Composable
private fun SnackbarMessages(
    messages: Flow<String>,
    hostState: SnackbarHostState
) {
    LaunchedEffect(messages) {
        messages.collect { message -> hostState.showSnackbar(message) }
    }
}
