package com.fitbudget.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitbudget.app.di.AppContainer
import com.fitbudget.app.notifications.NotificationHelper
import com.fitbudget.app.ui.AppViewModelFactory
import com.fitbudget.app.ui.LocalAppViewModelFactory
import com.fitbudget.app.ui.navigation.FitBudgetNavHost
import com.fitbudget.app.ui.screens.root.RootViewModel
import com.fitbudget.app.ui.theme.FitBudgetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private lateinit var viewModelFactory: AppViewModelFactory

    /** Route requested by a tapped notification, consumed once by the nav host. */
    private var pendingRoute by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            lifecycleScope.launch {
                if (granted) {
                    // Re-arm everything now that we are allowed to post notifications.
                    runCatching { container.reminderRepository.rescheduleAll() }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        container = AppContainer.from(this)
        viewModelFactory = AppViewModelFactory(container)
        pendingRoute = intent?.getStringExtra(NotificationHelper.EXTRA_ROUTE)

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            val rootViewModel: RootViewModel = viewModel(factory = viewModelFactory)
            val state by rootViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.loading) {
                if (!state.loading) keepSplash = false
            }

            // Ask for notification permission once the user actually has a plan to be reminded about.
            LaunchedEffect(state.onboardingComplete) {
                if (state.onboardingComplete &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.hasPermission(this@MainActivity)
                ) {
                    runCatching {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            FitBudgetTheme(
                themeMode = state.settings.themeMode,
                dynamicColor = state.settings.dynamicColorEnabled
            ) {
                CompositionLocalProvider(LocalAppViewModelFactory provides viewModelFactory) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (state.loading) {
                            // Splash is still up; render an empty branded surface underneath.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        } else {
                            val route = remember(pendingRoute) { pendingRoute }
                            FitBudgetNavHost(
                                onboardingComplete = state.onboardingComplete,
                                pendingDeepLink = route,
                                onDeepLinkHandled = { pendingRoute = null }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_ROUTE)?.let { pendingRoute = it }
    }

    override fun onResume() {
        super.onResume()
        // Reading the hardware step counter while the app is in the foreground keeps step data
        // current without a battery-hungry foreground service.
        runCatching { container.stepSensorManager.start() }
    }

    override fun onPause() {
        runCatching { container.stepSensorManager.stop() }
        super.onPause()
    }
}
