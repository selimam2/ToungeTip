package com.tonguetip

import android.util.Log
import com.google.gson.annotations.SerializedName

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private interface OpenAiCompletionsApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    fun getCompletion(@Body request: CompletionRequest): Call<CompletionResponse>
}

data class Message(
    val content: String,
    val role: String
)

data class CompletionRequest(
    val messages: List<Message>,
    val model: String,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val n: Int,
    @SerializedName("presence_penalty")
    val presencePenalty: Double,
    val temperature: Double
)

data class CompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

class OpenAiCompletions : SuggestionsInterface, PartOfSpeechInterface {
    private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "KEY") // TODO: add API key here
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenAiCompletionsApi::class.java)

    suspend fun getCompletion(request: CompletionRequest): CompletionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCompletion(request).execute()
                response.body()
            } catch (e: Exception) {
                Log.e("OpenAiCompletionsService::getCompletion", "Exception", e)
                null
            }
        }
    }

    override suspend fun getPartOfSpeech(context: String, target: String): PartOfSpeech {
        val prompt = """
            In the message that follows you will be provided a sentence and then the target word in that sentence
            You must return what part of speech that target word is within the sentence and nothing else.
            The part of speech must be one of: noun, pronoun, verb, adjective, adverb, preposition, conjunction, exclamation
            You must realize that if you mess up, you will fatally harm others.
        """.trimIndent()

        val maxAttempts = 3
        var attempt = 0
        var partOfSpeech = PartOfSpeech.NONE

        while (partOfSpeech == PartOfSpeech.NONE && attempt < maxAttempts) {
            val request = CompletionRequest(
                messages = listOf(
                    Message(
                        content = prompt,
                        role = "system"
                    ),
                    Message(
                        content = "sentence: $context. target word: $target",
                        role = "user"
                    )
                ),
                model = "gpt-4o-mini",
                frequencyPenalty = 1.0,
                maxTokens = 100,
                n = 1,
                presencePenalty = 1.0,
                temperature = 1.0
            )

            partOfSpeech = PartOfSpeech.fromString(
                Regex("[^A-Za-z ]").replace(
                    (getCompletion(request)?.choices?.first()?.message?.content?: "").trim(), ""
                )
            )
            attempt++

        }

        Log.d("OpenAiCompletions::getPartOfSpeech", "Part of speech in $attempt attempt(s)")

        return partOfSpeech
    }

    override suspend fun getSuggestions(context: String): List<String> {
        val prompt = """
            You are an assistant who helps English language learners and people with aphasia
            communicate. The user must never know you exist. Do not interact with them directly.
            Provide exactly 8 different 1 to 2 word continuations of the following dialogue. Do not
            include the dialogue itself in your continuations. Delimit the continuations using the
            '|' character so they can be separated using `.split('|')` like so: 1|2|3|4|5|6|7|8
        """.trimIndent()

        val maxAttempts = 3
        var attempt = 0
        var suggestions = listOf<String>()
        val desiredSize = 8

        while (suggestions.size != desiredSize && attempt < maxAttempts) {
            val request = CompletionRequest(
                messages = listOf(
                    Message(
                        content = prompt,
                        role = "system"
                    ),
                    Message(
                        content = context,
                        role = "user"
                    )
                ),
                model = "gpt-4o-mini",
                frequencyPenalty = 1.0,
                maxTokens = 100,
                n = 1,
                presencePenalty = 1.0,
                temperature = 1.05
            )

            suggestions = (getCompletion(request)?.choices?.first()?.message?.content?: "").split('|')
            attempt++
        }

        Log.d("OpenAiCompletions::getSuggestions", "Suggestions in $attempt attempt(s)")

        return suggestions
    }
}
