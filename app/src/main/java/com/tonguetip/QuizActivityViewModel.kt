package com.tonguetip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuestionTypes {
    FINISH_SENTENCE,
    DEFINE_WORD,
    NATIVE_TRANSLATION
}

data class QuizState(
    val forgottenWords: List<StringContext>? = null,
    val forgottenSentences: LinkedHashMap<StringContext, String>? = null,
    val translatedWords: LinkedHashMap<StringContext, String>? = null,
    val questions: MutableList<Question>? = null,
    val score: Int = 0,
    val index: Int = 0
)

data class Question(
    val questionType: QuestionTypes,
    val header: String,
    val answer: String?,
    val partOfSpeech: PartOfSpeech
)

data class StringContext(
    val string: String,
    val partOfSpeech: PartOfSpeech
)

class QuizActivityViewModel(nativeLanguage: String) : ViewModel() {
    private val _uiState = MutableStateFlow(QuizState())
    val uiState: StateFlow<QuizState> = _uiState.asStateFlow()
    private var translationIntegration:TranslationService? = null
    private val dictionaryIntegration = Dictionary()
    private var language: String = ""
    var currentQuestionIndex = 0
    var currentScore = 0
    init {
        language = nativeLanguage
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
            forgottenWordsList = forgottenWordsList.shuffled()
            var contextWordList = mutableListOf<StringContext>()
            for (word in forgottenWordsList) {
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
            if (language != TranslateLanguage.ENGLISH) {
                if (translationIntegration != null) {
                    for (index in 10..14) {
                        if (index in contextWordList.indices) {
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
                    forgottenSentences = sentenceList,
                    translatedWords = translations,
                    questions = questionsList
                )
            }
        }
    }

    private suspend fun generateQuestions(forgottenWords: MutableList<StringContext>, forgottenSentences: LinkedHashMap<StringContext, String>?, translatedWords: LinkedHashMap<StringContext, String>): MutableList<Question> {
        val questionList = mutableListOf<Question>()
        if (forgottenWords != null) {
            for (index in 0..4) {
                if (index in forgottenWords.indices) {
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
        }
        if (forgottenSentences != null) {
            for ((key, value) in forgottenSentences) {
                val question = Question(
                    questionType = QuestionTypes.FINISH_SENTENCE,
                    header = key.string,
                    answer = value,
                    partOfSpeech = key.partOfSpeech
                )
                questionList.add(question)
            }
        }
        if (translatedWords != null) {
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
        }

        questionList.shuffle()

        return questionList
    }

    fun getCurrentQuestion(): Question? {
        return _uiState.value.questions?.getOrNull(currentQuestionIndex)
    }

    fun submitAnswer(answer: String) {
        val currentQuestion = getCurrentQuestion()
        if (currentQuestion != null && answer == currentQuestion.answer) {
            currentScore++
        }
        currentQuestionIndex++
        if (currentQuestionIndex >= (_uiState.value.questions?.size ?: 0)) {
            _uiState.update { currentState ->
                currentState.copy(questions = null)
            }
        } else {
            // Update state with new current question
            _uiState.update { currentState ->
                currentState.copy(
                    score = currentScore,
                    index = currentQuestionIndex
                )
            }
        }
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