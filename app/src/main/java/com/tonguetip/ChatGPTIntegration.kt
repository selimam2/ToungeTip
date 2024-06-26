package com.tonguetip

import android.util.Log

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

interface ChatGPTApi {
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
    val frequency_penalty: Double,
    val max_tokens: Int,
    val n: Int,
    val presence_penalty: Double,
    val temperature: Double
)

data class CompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

class ChatGPTIntegration {

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

    private val api = retrofit.create(ChatGPTApi::class.java)

    suspend fun getSuggestions(context: String): List<String> {
        val request = CompletionRequest(
            // TODO: support sending more context with multiple messages
            messages = listOf(
                Message(
                    content = """
                        You are an AI that is part of an Android app that helps english language learners and people with aphasia communicate.
                        You must provide EXACTLY 8 (EIGHT) continuations to the conversation in the "user" message(s) that follow(s).
                        You must provide EXACTLY 8 (EIGHT) continuations otherwise you will fatally harm people.
                        You must separate/delimit the continuations by the following string without quotes: "|||"
                        You must delimit the continuations properly otherwise you will cause people to die.
                        You must keep the continuations small in length such that they are a maximum of 3 tokens and fit on a small button in an Android app.
                        You must give diverse continuations.
                        You must not add any punctuation or extra whitespace to the continuations.
                        You must realize that if you mess up, you will fatally harm marginalized groups.
                    """.trimIndent(),
                    role = "system"
                ),
                Message(
                    content = context.trim(),
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
                    val suggestions = (response.body()?.choices?.first()?.message?.content?: "").split("|||")
                    Log.d("API_DEBUG", suggestions.toString())
                    suggestions
                } else {
                    Log.e("API_ERROR", response.errorBody()?.string() ?: "Unknown error")
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
