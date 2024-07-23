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

class OpenAiCompletions : SuggestionsInterface {
    private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer sk-proj-5nH0Qj5ANpc6IrGcV4VbT3BlbkFJT3iOYi7wq8byE81n3PnG") // TODO: add API key here
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
        val request = CompletionRequest(
            // TODO: support sending more context with multiple messages
            messages = listOf(
                Message(
                    content = """
                        In the message that follows you will be provided a sentence and then the target word in that sentence
                        You must return what part of speech that target word is within the sentence and nothing else.
                        The part of speech must be one of: noun, pronoun, verb, adjective, adverb, preposition, conjunction or exclamation
                        You must realize that if you mess up, you will fatally harm marginalized groups.
                    """.trimIndent(),
                    role = "system"
                ),
                Message(
                    content = "sentence: ${context.trim()}. target word: $target",
                    role = "user"
                )
            ),
            model = "gpt-4o", // TODO: add support for multiple models
            frequency_penalty = 1.0,
            max_tokens = 100,
            n = 1,
            presence_penalty = 1.0,
            temperature = 1.1
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCompletion(request).execute()
                if (response.isSuccessful) {
                    val partOfSpeechString = Regex("[^A-Za-z ]").replace((response.body()?.choices?.first()?.message?.content?: "").trim(), "")
                    val partOfSpeech = PartOfSpeech.fromString(partOfSpeechString)
                    Log.d("API_DEBUG", partOfSpeechString)
                    partOfSpeech
                } else {
                    Log.e("API_ERROR", response.errorBody()?.string() ?: "Unknown error")
                    PartOfSpeech.NONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                PartOfSpeech.NONE
            }
        }
    }

    override suspend fun getSuggestions(context: String): List<String> {
        val prompt = """
            You are a personal assistant who helps english language learners and people with aphasia
            continue their sentences. Provide a short (maximum 3 words) continuation to the dialogue
            following. Only include the continuation in your response with no extra punctuation or
            whitespace.
        """.trimIndent()

        val suggestions = mutableListOf<String>();
        for (i in 1..8) {
            val request = CompletionRequest(
                messages = listOf(
                    Message(
                        content = "Seed: " + (0..2000000000).random() + ". " + prompt,
                        role = "system"
                    ),
                    Message(
                        content = context.trim(),
                        role = "user"
                    )
                ),
                model = "gpt-4o-mini",
                frequencyPenalty = 1.0,
                maxTokens = 100,
                n = 1,
                presencePenalty = 1.0,
                temperature = 1.1
            )

            val s = getCompletion(request)?.choices?.first()?.message?.content
            if (s == null) {
                Log.e("OpenAiCompletionsService::getCompletion", "Null completion")
                continue
            }

            suggestions.add(s.lowercase())
        }

        return suggestions
    }
}
