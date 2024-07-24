package com.tonguetip

import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream

class LocalGemma(context: android.content.Context) : SuggestionsInterface, PartOfSpeechInterface {
    private val MODEL_PATH = "/data/local/tmp/llm/"
    private val MODEL_FILE_NAME = "gemma-2b-it-gpu-int4.bin"
    private var llm : LlmInference
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

        // Set the configuration options for the LLM Inference task
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH + MODEL_FILE_NAME)
            .setTopK(40)
            .setMaxTokens(50)
            .setTemperature(1.5f)
            .setRandomSeed((0..2000000000).random())
            .build()
        ctx = context
        // Create an instance of the LLM Inference task
        llm = LlmInference.createFromOptions(context, options)
    }

    override suspend fun getPartOfSpeech(context: String, target: String): PartOfSpeech {
        return PartOfSpeech.NONE // TODO: Add gemma version of this
    }

    override suspend fun getSuggestions(context: String): List<String> {
        val prompt = """
            Only output a continuation of a single word for the following dialogue.
        """.trimIndent()
        val context1 = context.trim()
        var suggestions = mutableListOf<String>()
        for (i in 1..8)
        {
            val request = prompt + context1
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH + MODEL_FILE_NAME)
                .setTopK(40)
                .setMaxTokens(llm.sizeInTokens(request) + 4)
                .setTemperature(1.5f)
                .setRandomSeed((0..2000000000).random())
                .build()
            llm.close()
            llm = LlmInference.createFromOptions(ctx, options)
            suggestions.add(llm.generateResponse(request))
        }
        for (j in suggestions)
        {
            println(j)
            println("Done")
        }



        llm.close()
        Log.d("GemmaCompletions::getSuggestions", "Suggestions in  attempt(s)")

        return suggestions
    }
}