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

data class DetailedSuggestionState(
    val words: List<DictionaryEntry>? = null,
)
class DetailedSuggestionActivityViewModel(private val suggestion : String) : ViewModel() {
    private val dictionaryIntegration = Dictionary()

    private val _uiState = MutableStateFlow(DetailedSuggestionState())
    val uiState: StateFlow<DetailedSuggestionState> = _uiState.asStateFlow()

    private val re = Regex("[^A-Za-z ]")

    var suggestions: List<String> = emptyList()

    init {
        updateSuggestionDictionary()
    }
    fun updateSuggestionDictionary() {
        viewModelScope.launch {
            val trimmedSuggestion = suggestion.trim()
            val stripSpecial = re.replace(trimmedSuggestion, "")
            suggestions = stripSpecial.split("\\s+".toRegex())
            val entries = mutableListOf<DictionaryEntry>()
            for (sug in suggestions){
                val entry = dictionaryIntegration.getDictionaryEntry(sug) ?: continue
                entries.add(entry)
            }
            _uiState.update { currentState ->
                currentState.copy(
                    words = entries
                )
            }
        }
    }

    fun hasMultipleWords(): Boolean {
        return suggestions.size > 1
    }

    fun insertSuggestionToDatabase(suggestion: String, contextString: String){
        viewModelScope.launch{
            DatabaseHandler.addSuggestion(suggestion, SuggestionContext(contextString, LocalDate.now()))
        }
    }
}