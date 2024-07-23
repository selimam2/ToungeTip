package com.tonguetip

interface ILLM {
    abstract suspend fun getSuggestions(context: String): List<String>
    abstract suspend fun getPartOfSpeech(context: String, target: String): PartOfSpeech
}