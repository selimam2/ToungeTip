package com.tonguetip

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate



class SettingsActivityViewModel(application: Application) : AndroidViewModel(application) {
    fun insertDemoSuggestions(){
        viewModelScope.launch{
            DatabaseHandler.loadDemoSuggestions()
        }
    }

}