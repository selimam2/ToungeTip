package com.tonguetip
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestion
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage

class SmartReply : SuggestionsInterface {
    val smartReply = SmartReply.getClient()
    val conversation  = listOf(
        TextMessage.createForLocalUser("Hello, I'm struggling to find the right words.", 20),
        TextMessage.createForLocalUser("Could you help me with some suggestions?", 5020)
    )
    init {

    }
    override suspend fun getSuggestions(context: String): List<String> {
        smartReply.suggestReplies(conversation)
            .addOnSuccessListener { result ->
                if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    for (suggestion: SmartReplySuggestion in result.suggestions) {
                        Log.d("SmartReply", suggestion.text)
                        // Use the suggestions in your app
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("SmartReply", "Error: ${e.message}")
            }
    }
}