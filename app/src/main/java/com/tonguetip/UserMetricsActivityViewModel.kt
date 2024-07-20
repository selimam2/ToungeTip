package com.tonguetip

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UserMetricsState(
    val metrics: List<Metric>? = null,
)
class UserMetricsActivityViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(UserMetricsState())
    val uiState: StateFlow<UserMetricsState> = _uiState.asStateFlow()
    init {
        updateMetrics()
    }
    private fun updateMetrics() {
        viewModelScope.launch {
            val metricList = mutableListOf<Metric>()
            metricList.add(MostForgottenWordMetricFactory().makeMetric())
            metricList.add(ForgottenWordOfTheDayMetricFactory().makeMetric())
            metricList.add(NumberOfSuggestionsUsedMetricFactory().makeMetric())
            metricList.add(MostRecentlyUsedMetricFactory().makeMetric())
            metricList.add(LeastRecentlyUsedMetricFactory().makeMetric())
            _uiState.update { currentState ->
                currentState.copy(
                    metrics = metricList
                )
            }
        }
    }
}