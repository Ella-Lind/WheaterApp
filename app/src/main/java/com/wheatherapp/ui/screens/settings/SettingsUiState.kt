package com.wheatherapp.ui.screens.settings

import com.wheatherapp.domain.model.UserSettings

/**
 * Immutable UI State for the Settings Screen.
 */
data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)
