package com.tonguetip

import android.content.Context
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

    private lateinit var suggester: SuggestionsInterface

    private val _uiState = MutableStateFlow(MainUIState())
    val uiState: StateFlow<MainUIState> = _uiState.asStateFlow()

    // Handle business logic

    fun buttonTest(ctx: Context) {
        // Strategy design pattern: Choose AI backend based on user preferences
        // Store the backend as an implementor of SuggestionsInterface
        val sharedPrefs = ctx.getSharedPreferences("TONGUETIP_SETTINGS", Context.MODE_PRIVATE)
        when (sharedPrefs.getString("LLMOption", "ChatGPT")) {
            "ChatGPT" -> {
                suggester = OpenAiCompletions()
            }

            "Gemma" -> {
                suggester = LocalGemma(ctx)
            }
        }

        viewModelScope.launch {
            val suggestions = suggester.getSuggestions(_uiState.value.liveTextString)
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