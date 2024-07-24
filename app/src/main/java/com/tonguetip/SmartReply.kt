package com.tonguetip
import android.util.Log
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestion
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage

class SmartReply : SuggestionsInterface {


    init {

    }
    override suspend fun getSuggestions(context: String): List<String> {
        val conversation  = listOf(
            TextMessage.createForLocalUser("I'm struggling to find the right words in the following dialogue, please suggest some words", System.currentTimeMillis()),
            TextMessage.createForLocalUser(context, System.currentTimeMillis())
        )
        val smartReply = SmartReply.getClient()
        val replies = smartReply.suggestReplies(conversation).await().await()?.suggestions?.toString()?: ""

        Log.d("SmartReply::getSuggestions", replies)

        return listOf()
    }
}