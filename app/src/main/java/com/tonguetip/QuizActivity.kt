package com.tonguetip

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val currentQuestion = viewModel.getCurrentQuestion()

    if (currentQuestion != null) {
        QuestionView(
            question = currentQuestion,
            onAnswerSelected = { answer ->
                viewModel.submitAnswer(answer)
            }
        )
    } else {
        // No more questions
        Text("Quiz finished!", modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun QuestionView(question: Question, onAnswerSelected: (String) -> Unit) {
    val options = remember { generateOptions(question.answer ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = question.header,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(options) { option ->
                AnswerTile(answer = option, onClick = onAnswerSelected)
            }
        }
    }
}

@Composable
fun AnswerTile(answer: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray, shape = MaterialTheme.shapes.medium)
            .clickable { onClick(answer) }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = answer, fontSize = 18.sp)
    }
}

fun generateOptions(correctAnswer: String): List<String> {
    val options = mutableListOf(correctAnswer, "Option 1", "Option 2", "Option 3")
    options.shuffle()
    return options
}