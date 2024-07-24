package com.tonguetip

import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.FileOutputStream

class LocalGemma(context: android.content.Context) : SuggestionsInterface, PartOfSpeechInterface {
    private val MODEL_PATH = "/data/local/tmp/llm/"
    private val MODEL_FILE_NAME = "gemma-2b-it-gpu-int4.bin"
    private var llm : LlmInference

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
            .setMaxTokens(1000)
            .setTopK(40)
            .setTemperature(0.2f)
            .setRandomSeed((0..2000000000).random())
            .build()

        // Create an instance of the LLM Inference task
        llm = LlmInference.createFromOptions(context, options)
    }

    override suspend fun getPartOfSpeech(context: String, target: String): PartOfSpeech {
        return PartOfSpeech.NONE // TODO: Add gemma version of this
    }

    override suspend fun getSuggestions(context: String): List<String> {
        val suggestions = mutableListOf<String>()
        for (i in (1..8)) {
            val content = "Seed: " + (0..2000000000).random() + ". Complete the following dialogue using as few words as possible: " + context
            suggestions.add(llm.generateResponse(content))
        }

        return suggestions
    }
}