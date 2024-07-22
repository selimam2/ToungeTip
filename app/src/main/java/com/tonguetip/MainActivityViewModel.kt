package com.tonguetip

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class MainUIState(
    val isListening: Boolean = true,
    val liveTextString: String = "",
    var suggestions: List<String>? = null,
)

class MainActivityViewModel : ViewModel() {

    private val chatGPTIntegration = ChatGPTIntegration()

    private val _uiState = MutableStateFlow(MainUIState())
    val uiState: StateFlow<MainUIState> = _uiState.asStateFlow()

    // Handle business logic

    fun buttonTest(ctx : android.content.Context) {
        viewModelScope.launch {
            val gemma = GemmaIntegration(ctx)

            val suggestions = chatGPTIntegration.getSuggestions(_uiState.value.liveTextString)
            _uiState.update { currentState ->
                currentState.copy(
                    isListening = !currentState.isListening,
                    liveTextString = currentState.liveTextString,
                    suggestions = suggestions
                )
            }
        }
    }

    fun updateLiveText(str: String){
        _uiState.update { currentState ->
            currentState.copy(
                isListening = currentState.isListening,
                liveTextString = str,
                suggestions = currentState.suggestions
            )
        }
    }

    fun suggestionReset() {
        _uiState.update { currentState ->
            currentState.copy(
                suggestions = null,
                liveTextString = ""
            )
        }
    }
}