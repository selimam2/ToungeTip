package com.tonguetip

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainUIState(
    val isListening: Boolean = true,
    val liveTextString: String = "",
    var suggestions: List<String>? = null,
)
class MainActivityViewModel : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(MainUIState())
    val uiState: StateFlow<MainUIState> = _uiState.asStateFlow()

    // Handle business logic
    fun buttonTest() {
        _uiState.update { currentState ->
            currentState.copy(
                isListening = !currentState.isListening,
                liveTextString = "Button Pressed",
                suggestions = listOf("Fries", "Pasta", "Salad", "Tacos", "Sushi", "Pizza", "Beans", "Burgers")
            )
        }
    }

    fun suggestionReset() {
        _uiState.update { currentState ->
            currentState.copy(
                suggestions = null
            )
        }
    }
}