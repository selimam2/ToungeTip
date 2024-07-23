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
    val translations: LinkedHashMap<String,String>? = null,
    val partsOfSpeech: LinkedHashMap<String, PartOfSpeech>? = null
)
class DetailedSuggestionActivityViewModel(private var suggestion : String,private var suggestionContext:String, nativeLanguage: String) : ViewModel() {
    private val gpt = ChatGPTIntegration()
    private val dictionaryIntegration = Dictionary()
    private var translationIntegration:TranslationService? = null
    private var shownWords: MutableList<String> = mutableListOf() // Mirror of detailedsuggestionstate words for use by translation

    private val _uiState = MutableStateFlow(DetailedSuggestionState())
    val uiState: StateFlow<DetailedSuggestionState> = _uiState.asStateFlow()

    private val re = Regex("[^A-Za-z ]")

    private var suggestions: List<String> = emptyList()

    init {
        val trimmedSuggestion = suggestion.trim()
        suggestion = re.replace(trimmedSuggestion, "")
        suggestions = suggestion.split("\\s+".toRegex())
        updatePartsOfSpeech()
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
     private fun updatePartsOfSpeech(){
         viewModelScope.launch {
             val partsOfSpeech = linkedMapOf<String, PartOfSpeech>()
             for (sug in suggestions) {
                 partsOfSpeech[suggestion] = (gpt.getPartOfSpeech(suggestionContext, suggestion))
             }
             _uiState.update { currentState ->
                 currentState.copy(
                     partsOfSpeech = partsOfSpeech
                 )
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

    fun insertSuggestionToDatabase(){
        viewModelScope.launch{
            val partOfSpeechOverall = (if(suggestions.size > 1 || _uiState.value.partsOfSpeech.isNullOrEmpty()) PartOfSpeech.NONE else _uiState.value.partsOfSpeech!![suggestions[0]]) ?: PartOfSpeech.NONE
            DatabaseHandler.addSuggestion(suggestion, SuggestionContext(suggestionContext, LocalDate.now(), partOfSpeechOverall))
        }
    }
}