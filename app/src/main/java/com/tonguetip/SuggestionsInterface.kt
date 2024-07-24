package com.tonguetip

interface SuggestionsInterface {
    suspend fun getSuggestions(context: String): List<String>
}