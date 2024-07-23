package com.tonguetip

interface PartOfSpeechInterface {
    abstract suspend fun getPartOfSpeech(context: String, target: String): PartOfSpeech
}
