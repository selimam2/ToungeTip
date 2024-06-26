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
    val role: String,
)

data class CompletionRequest(
    val messages: List<Message>,
    val model: String,
    val frequency_penalty: Double,
    val max_tokens: Int,
    val n: Int,
    val presence_penalty: Double,
    val temperature: Double,
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
            messages = listOf(context.trim()).map { s -> Message(
                content = s,
                role = "user"
            )}, // TODO: support sending more context with multiple messages
            model = "gpt-4o", // TODO: add support for multiple models
            frequency_penalty = 0.0,
            max_tokens = 5,
            n = 8,
            presence_penalty = 0.0,
            temperature = 2.0
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCompletion(request).execute()
                if (response.isSuccessful) {
                    val choices = response.body()?.choices
                    if (choices.isNullOrEmpty() || choices.size != 8) {
                        Log.e("API_ERROR", "Invalid response")
                        emptyList()
                    } else {
                        val suggestions = choices.map { c -> c.message.content }
                        Log.d("API_DEBUG", suggestions.toString())
                        suggestions
                    }
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
