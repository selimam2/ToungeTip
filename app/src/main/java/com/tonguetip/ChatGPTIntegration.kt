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
    @POST("v1/completions")
    fun getCompletion(@Body request: CompletionRequest): Call<CompletionResponse>
}

data class CompletionRequest(
    val model: String,
    val prompt: String,
    val max_tokens: Int,
    val temperature: Double,
)

data class CompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val text: String
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
        val prompt =
            """
            You help english language learners and people with aphasia communicate.
            You will be provided with the conversation context to output relevant suggestions that finish the sentence.
            You must not output anything but a list of 8 comma-separated words.
            You must not add any extra white space to the output.
            The conversation context follows the colon:\n
            """.trimIndent() + context.trim()

        val request = CompletionRequest(
            model = "gpt-3.5-turbo-instruct", // TODO: add support for multiple models
            prompt = prompt,
            max_tokens = 100,
            temperature = 0.5
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCompletion(request).execute()
                if (response.isSuccessful) {
                    val completionText = response.body()?.choices?.firstOrNull()?.text ?: ""
                    Log.e("API_DEBUG", completionText)
                    completionText.trim().split(",")
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
