package com.tonguetip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 3 types of questions
enum class QuestionTypes {
    FINISH_SENTENCE,
    DEFINE_WORD,
    NATIVE_TRANSLATION
}

data class QuizState(
    val forgottenWords: List<StringContext>? = null,
    val questions: MutableList<Question>? = null,
    val score: Int = 0,
    val index: Int = 0
)

// Must have various pieces of information for a single question
data class Question(
    val questionType: QuestionTypes,
    val header: String,
    val answer: String?,
    val partOfSpeech: PartOfSpeech
)

// Must know the word and part of speech
data class StringContext(
    val string: String,
    val partOfSpeech: PartOfSpeech
)

// Store question results
data class Result(
    val question: Question,
    val answer: String,
    val correctness: Boolean,
)

class QuizActivityViewModel(nativeLanguage: String) : ViewModel() {
    // State
    private val _uiState = MutableStateFlow(QuizState())
    val uiState: StateFlow<QuizState> = _uiState.asStateFlow()
    // Integrations for Quiz Questions
    private var translationIntegration:TranslationService? = null
    private val dictionaryIntegration = Dictionary()
    // Private vars to track information
    private var _language: String = ""
    private var _currentQuestionIndex = 0
    private var _currentScore = 0
    private var _resultList = mutableListOf<Result>()

    init {
        _language = nativeLanguage
        if(nativeLanguage != TranslateLanguage.ENGLISH){
            translationIntegration = TranslationService(TranslateLanguage.ENGLISH,nativeLanguage)
        }
        updateQuiz()
    }

    private fun updateQuiz() {
        viewModelScope.launch {
            // 5 words for questions asking to define a word
            // 5 sentences for questions asking to finish the sentence
            // 5 translation words (if needed)
            var forgottenWordsList = DatabaseHandler.getNPastSuggestions(15)

            if (forgottenWordsList.size < 15) {
                _uiState.update { currentState ->
                    currentState.copy(
                        forgottenWords = null,
                        questions = null,
                        index = -1
                    )
                }
                return@launch
            }

            forgottenWordsList = forgottenWordsList.shuffled()
            val contextWordList = mutableListOf<StringContext>()
            for (word in forgottenWordsList) {
                // Get the context used for the word for part of speech
                val context = DatabaseHandler.getContextForSuggestion(word, 1)
                val wordContext = StringContext(
                    string = word,
                    partOfSpeech = context[0].partOfSpeech
                )
                contextWordList.add(wordContext)
            }

            val sentenceList = linkedMapOf<StringContext,String>()
            // Indices 5-9 for sentences
            for (index in 5..9) {
                if (index in forgottenWordsList.indices) {
                    // Get the context used for the word for part of speech and sentence used
                    val forgottenSentence = DatabaseHandler.getContextForSuggestion(forgottenWordsList[index], 1)
                    val sentence = StringContext(
                        string = forgottenSentence[0].contextString,
                        partOfSpeech = forgottenSentence[0].partOfSpeech
                    )
                    sentenceList[sentence] = forgottenWordsList[index]
                }
            }

            // Indices 10-14 for translation
            val translations = linkedMapOf<StringContext,String>()
            if (_language != TranslateLanguage.ENGLISH) {
                if (translationIntegration != null) {
                    for (index in 10..14) {
                        if (index in contextWordList.indices) {
                            // Get the translation of the word
                            val word = contextWordList[index]
                            val translation = translationIntegration!!.translate(word.string)
                            translations[word] = translation
                        }
                    }
                    translationIntegration?.close()
                    translationIntegration = null
                }
            }

            val questionsList = generateQuestions(contextWordList, sentenceList, translations)

            _uiState.update { currentState ->
                currentState.copy(
                    forgottenWords = contextWordList,
                    questions = questionsList
                )
            }
        }
    }

    private suspend fun generateQuestions(forgottenWords: MutableList<StringContext>, forgottenSentences: LinkedHashMap<StringContext, String>, translatedWords: LinkedHashMap<StringContext, String>): MutableList<Question> {
        val questionList = mutableListOf<Question>()
        for (index in 0..4) {
            if (index in forgottenWords.indices) {
                // Get the definition of the word
                val word = forgottenWords[index]
                val entry = dictionaryIntegration.getDictionaryEntry(word.string) ?: continue
                val ans = entry.mainDefinition?.definition
                val question = Question(
                    questionType = QuestionTypes.DEFINE_WORD,
                    header = word.string,
                    answer = ans,
                    partOfSpeech = word.partOfSpeech
                )
                questionList.add(question)
            }
        }

        for ((key, value) in forgottenSentences) {
            val question = Question(
                questionType = QuestionTypes.FINISH_SENTENCE,
                header = key.string,
                answer = value,
                partOfSpeech = key.partOfSpeech
            )
            questionList.add(question)
        }

        for ((key, value) in translatedWords) {
            // Header is set to translated word, options will be in english
            val question = Question(
                questionType = QuestionTypes.NATIVE_TRANSLATION,
                header = value,
                answer = key.string,
                partOfSpeech = key.partOfSpeech
            )
            questionList.add(question)
        }

        questionList.shuffle()

        return questionList
    }

    fun getCurrentQuestion(): Question? {
        return _uiState.value.questions?.getOrNull(_currentQuestionIndex)
    }

    fun getCurrentQuestionIndex(): Int {
        return _currentQuestionIndex
    }

    fun submitAnswer(answer: String) {
        val currentQuestion = getCurrentQuestion()

        // Set score
        if (currentQuestion != null && answer == currentQuestion.answer) {
            _currentScore++
        }

        // Remember result
        if (currentQuestion != null) {
            val result = Result(
                question = currentQuestion,
                answer = answer,
                correctness = answer == currentQuestion.answer
            )
            _resultList.add(result)
        }

        _currentQuestionIndex++

        // Check if at the end of available questions
        if (_currentQuestionIndex >= (_uiState.value.questions?.size ?: 0)) {
            _uiState.update { currentState ->
                currentState.copy(index = -1)
            }
        } else {
            // Update state with new question
            _uiState.update { currentState ->
                currentState.copy(
                    score = _currentScore,
                    index = _currentQuestionIndex
                )
            }
        }
    }

    fun getResults(): MutableList<Result> {
        return _resultList
    }

    suspend fun generateOptionsForDefinition(answer: String, list: List<String>): List<String> {
        val options = mutableListOf<String>()
        for (item in list) {
            if (item == answer) continue // Skip the correct answer
            val entry = dictionaryIntegration.getDictionaryEntry(item) ?: continue
            val ans = entry.mainDefinition?.definition ?: continue

            options.add(ans)
        }

        if (!options.contains(answer)) {
            options.add(answer)
        }

        options.shuffle()

        return options
    }
}