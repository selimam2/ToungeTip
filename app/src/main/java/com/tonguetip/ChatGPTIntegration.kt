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
    val top_p: Double
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
                .addHeader("Authorization", "Bearer API_KEY_DO_NOT_PUSH") // TODO: add API key here
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

}
