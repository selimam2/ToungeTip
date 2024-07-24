package com.tonguetip

import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream

class LocalGemma(context: android.content.Context) : SuggestionsInterface, PartOfSpeechInterface {
    private val MODEL_PATH = "/data/local/tmp/llm/"
    private val MODEL_FILE_NAME = "gemma-2b-it-gpu-int4.bin"
    private val ctx : android.content.Context

    init {
        val modelPath = File(MODEL_PATH + MODEL_FILE_NAME)
        if (!modelPath.exists()) {
            context.assets.open(MODEL_FILE_NAME).use { inputStream ->
                FileOutputStream(MODEL_PATH).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        ctx = context
    }

    override suspend fun getPartOfSpeech(context: String, target: String): PartOfSpeech {
        return PartOfSpeech.NONE // TODO: Add gemma version of this
    }

    override suspend fun getSuggestions(context: String): List<String> {
        val prompt = """
            Only output a continuation of a single word for the following dialogue. 
        """.trimIndent()

        val suggestions = mutableListOf<String>()
        val request = prompt + context

        var options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH + MODEL_FILE_NAME)
            .setTopK(40)
            .setMaxTokens(200)
            .setTemperature(1.5f)
            .setRandomSeed((0..2000000000).random())
            .build()
        var llm = LlmInference.createFromOptions(ctx, options)
        val maxTokens = llm.sizeInTokens(request) + 5
        llm.close()

        for (i in 1..4) {
            options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH + MODEL_FILE_NAME)
                .setTopK(40)
                .setMaxTokens(maxTokens)
                .setTemperature(1.5f)
                .setRandomSeed((0..2000000000).random())
                .build()


            try {
                llm = LlmInference.createFromOptions(ctx, options)
                suggestions.add(llm.generateResponse(request, ).split("[\\.,\\s]".toRegex()).first())
            } catch (e : Exception) {
                Log.e("LocalGemma::getSuggestions", "Exception", e)
            }

            llm.close()

        }

        Log.d("LocalGemma::getSuggestions", "Suggestions: $suggestions")

        return suggestions.distinct()
    }
}