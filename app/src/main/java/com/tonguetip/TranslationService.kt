package com.tonguetip

import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.common.model.DownloadConditions

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

fun <T> Task<T>.await(): Deferred<T> {
    val deferred = CompletableDeferred<T>()

    this.addOnSuccessListener { result ->
        deferred.complete(result)
    }

    this.addOnFailureListener { exception ->
        deferred.completeExceptionally(exception)
    }

    return deferred
}

class TranslationService(sourceLang: String, targetLang: String) {
    private var translator: Translator

    init {
        val options =
            TranslatorOptions.Builder().setSourceLanguage(sourceLang).setTargetLanguage(targetLang)
                .build()
        translator = Translation.getClient(options)
    }

    suspend fun translate(text: String): String {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build()).await()
            .await()
        return translator.translate(text).await().await()
    }

    fun close() {
        translator.close()
    }
}