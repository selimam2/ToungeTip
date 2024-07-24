
package com.tonguetip

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonguetip.DatabaseHandler.Companion.getContextForSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Collections
import java.util.SortedSet
import java.util.TreeSet

data class UserDictionaryState(
    val dictionary: MutableMap<String, MutableSet<String>>? = null,
)
class UserDictionaryActivityViewModel() : ViewModel(), DefaultLifecycleObserver {
    private val _uiState = MutableStateFlow(UserDictionaryState())
    val uiState: StateFlow<UserDictionaryState> = _uiState.asStateFlow()
    init {
        updateDictionary()
    }
    private fun updateDictionary() {
        viewModelScope.launch {
            val dict: MutableMap<String, MutableSet<String>> = mutableMapOf<String, MutableSet<String>>()
            val prevSuggestions = DatabaseHandler.getNPastSuggestions()
            for(suggestion in prevSuggestions){
                val contexts = getContextForSuggestion(suggestion)
                val words = suggestion.split("\\s+".toRegex())
                for(word in words){
                    if(dict.containsKey(word)){
                        for(context in contexts){
                            dict[word]!!.add(context.contextString)
                        }
                    }
                    else{
                        val contextStrings = mutableSetOf<String>()
                        for(context in contexts){
                            contextStrings.add(context.contextString)
                        }
                        dict[word] = contextStrings

                    }
                }
            }

            _uiState.update { currentState ->
                currentState.copy(
                    dictionary = dict
                )
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        updateDictionary()
    }
}