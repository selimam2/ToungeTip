package com.tonguetip

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class SettingsState(
    val idkIfNeeded:Int = 0,
)
class SettingsActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UserMetricsState())
    val uiState: StateFlow<UserMetricsState> = _uiState.asStateFlow()

}