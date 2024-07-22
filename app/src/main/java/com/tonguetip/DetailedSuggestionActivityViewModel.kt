package com.tonguetip

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.animation.core.updateTransition
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DetailedSuggestionState(
    val words: List<DictionaryEntry>? = null,
    val translations: LinkedHashMap<String,String>? = null
)
class DetailedSuggestionActivityViewModel(private val suggestion : String, nativeLanguage: String) : ViewModel() {
    private val dictionaryIntegration = Dictionary()
    private var translationIntegration:TranslationService? = null
    private var shownWords: MutableList<String> = mutableListOf() // Mirror of detailedsuggestionstate words for use by translation

    private val _uiState = MutableStateFlow(DetailedSuggestionState())
    val uiState: StateFlow<DetailedSuggestionState> = _uiState.asStateFlow()

    private val re = Regex("[^A-Za-z ]")

    var suggestions: List<String> = emptyList()

    init {
        val trimmedSuggestion = suggestion.trim()
        val stripSpecial = re.replace(trimmedSuggestion, "")
        suggestions = stripSpecial.split("\\s+".toRegex())
        updateSuggestionDictionary()
        if(nativeLanguage != TranslateLanguage.ENGLISH){
            translationIntegration = TranslationService(TranslateLanguage.ENGLISH,nativeLanguage)
            updateTranslation()
        }
    }

    private fun updateTranslation(){
        viewModelScope.launch {
            if (translationIntegration != null) {
                val translations = linkedMapOf<String,String>()
                for(word in suggestions){
                    val translation = translationIntegration!!.translate(word)
                    translations[word] = translation
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        translations = translations
                    )
                }
                translationIntegration?.close()
                translationIntegration = null
            }
        }
    }
    private fun updateSuggestionDictionary() {
        viewModelScope.launch {
            val entries = mutableListOf<DictionaryEntry>()
            for (sug in suggestions){
                val entry = dictionaryIntegration.getDictionaryEntry(sug) ?: continue
                shownWords.add(entry.word)
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