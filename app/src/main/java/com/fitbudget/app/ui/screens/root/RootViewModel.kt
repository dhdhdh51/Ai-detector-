package com.fitbudget.app.ui.screens.root

import androidx.lifecycle.viewModelScope
import com.fitbudget.app.data.repository.ProfileRepository
import com.fitbudget.app.data.settings.AppSettings
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RootUiState(
    val loading: Boolean = true,
    val onboardingComplete: Boolean = false,
    val settings: AppSettings = AppSettings()
)

/** Decides the start destination and supplies the theme to the whole app. */
class RootViewModel(
    profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel() {

    val uiState: StateFlow<RootUiState> = combine(
        profileRepository.profile,
        settingsRepository.settings
    ) { profile, settings ->
        RootUiState(
            loading = false,
            onboardingComplete = profile?.onboardingComplete == true,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RootUiState()
    )
}
