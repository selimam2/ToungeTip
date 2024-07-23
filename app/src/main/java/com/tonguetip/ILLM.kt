package com.tonguetip

interface ILLM {
    abstract suspend fun getSuggestions(context: String): List<String>
}