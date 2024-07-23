package com.tonguetip

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import com.tonguetip.ui.theme.TongueTipTheme

class QuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreference =  this.getSharedPreferences("TONGUETIP_SETTINGS", Context.MODE_PRIVATE)
        val nativeLang = sharedPreference.getString("NativeLanguage", TranslateLanguage.ENGLISH)
        setContent{
            TongueTipTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if(nativeLang != null){
                        QuizScreen(nativeLang)
                    }
                    else{
                        QuizScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun QuizScreen(
    nativeLang: String = TranslateLanguage.ENGLISH,
    viewModel: QuizActivityViewModel = viewModel{QuizActivityViewModel(nativeLang)}
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)
    val translatedWords = uiState.translatedWords
    val forgottenWords = uiState.forgottenWords
    val forgottenSentences = uiState.forgottenSentences

}