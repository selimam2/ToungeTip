package com.tonguetip

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                HamburgerMenu(context = this, "Quiz") {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        if (nativeLang != null) {
                            QuizScreen(nativeLang)
                        } else {
                            QuizScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        DatabaseHandler.closeDatabase()
        super.onStop()
    }
}

@Composable
fun QuizScreen(
    nativeLang: String = TranslateLanguage.ENGLISH,
    viewModel: QuizActivityViewModel = viewModel{QuizActivityViewModel(nativeLang)}
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)
    val forgottenWords = uiState.forgottenWords
    val currentQuestion = viewModel.getCurrentQuestion()
    val score = viewModel.currentScore
    val currentIndex = viewModel.currentQuestionIndex

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Display the score at the top
        Text(
            text = "Score: $score / $currentIndex",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Display the current question or the finish message
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .weight(1f) // Take up remaining space
        ) {
            if (currentQuestion == null && uiState.index != -1) {
                // Show loading indicator if the questions are still being fetched
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (currentQuestion != null) {
                QuestionView(
                    question = currentQuestion,
                    forgottenWords = forgottenWords,
                    viewModel = viewModel,
                    onAnswerSelected = { answer ->
                        viewModel.submitAnswer(answer)
                    }
                )
            } else if (uiState.index == -1){
                // No more questions
                ResultsView(results = viewModel.getResults())
            }
        }
    }
}

@Composable
fun QuestionView(question: Question, forgottenWords: List<StringContext>?, viewModel: QuizActivityViewModel, onAnswerSelected: (String) -> Unit) {
    // State to hold the options
    val options = remember { mutableStateOf<List<String>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) } // State to track loading

    var ans = StringContext(
        string = question.answer!!,
        partOfSpeech = question.partOfSpeech
    )
    if (question.questionType == QuestionTypes.DEFINE_WORD) {
        ans = StringContext(
            string = question.header,
            partOfSpeech = question.partOfSpeech
        )
    }
    val initialOptions = generateOptions(ans, forgottenWords)

    // Set initial options and start loading
    LaunchedEffect(question, forgottenWords) {
        if (question.questionType == QuestionTypes.DEFINE_WORD) {
            options.value = viewModel.generateOptionsForDefinition(
                answer = question.answer,
                list = initialOptions
            )
        } else {
            options.value = initialOptions
        }
        isLoading.value = false
    }

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

        if (isLoading.value) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(options.value) { option ->
                    AnswerTile(answer = option, onClick = onAnswerSelected)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnswerTile(answer: String, onClick: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray, shape = MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = { onClick(answer) }, // Regular click
                onLongClick = { showDialog = true } // Long press
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = answer,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Full Answer") },
            text = { Text(answer) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ResultsView(results: List<Result>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Results",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn {
            items(results) { result ->
                ResultItem(result)
            }
        }
    }
}

@Composable
fun ResultItem(result: Result) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray, shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(
            text = "Question: ${result.question.header}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp),
            color = Color.Black
        )
        Text(
            text = "Your Answer: ${result.answer}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp),
            color = if (result.correctness) Color(0xFF388E3C) else Color.Red
        )
        if (!result.correctness) {
            Text(
                text = "Correct Answer: ${result.question.answer}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }
    }
}


fun generateOptions(correctAnswer: StringContext, forgottenWords: List<StringContext>?): List<String> {
    val options = mutableSetOf(correctAnswer.string)

    // Ensure forgottenWords is not null and filter out the correct answer
    val wordsListNoAnswer = forgottenWords?.filter {
        it.string != correctAnswer.string
    } ?: emptyList()

    val wordsList = wordsListNoAnswer.filter{
        it.partOfSpeech != correctAnswer.partOfSpeech
    } ?: emptyList()

    // Randomly select 3 words from the list, if there are enough words
    val additionalOptions = if (wordsList.size >= 3) {
        wordsList.shuffled().take(3)
    } else {
        wordsList // If less than 3, take as many as available
    }

    options.addAll(additionalOptions.map { it.string })

    // Convert to list and shuffle
    return options.toList().shuffled()
}