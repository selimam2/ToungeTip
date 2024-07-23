package com.tonguetip

enum class PartOfSpeech(val value: String){
    NOUN("noun"),
    PRONOUN("pronoun"),
    VERB("verb"),
    ADJECTIVE("adjective"),
    ADVERB("adverb"),
    PREPOSITION("preposition"),
    CONJUNCTION("conjunction"),
    EXCLAMATION("exclamation"),
    NONE ("none");

    companion object {
        fun fromString(value: String):PartOfSpeech {
            return entries.find { it.value == value } ?: NONE
        }
    }
}
data class Definition(
    val definition: String,
    val example: String?,
    val synonyms: List<String>?,
    val antonyms: List<String>?
)
data class DictionaryEntry(
    val word: String,
    val pronunciationURL: String?,
    val meanings: LinkedHashMap<PartOfSpeech,List<Definition>>?,
    val mainDefinition: Definition?
)
interface DictionaryInterface {
    suspend fun getDictionaryEntry(word: String): DictionaryEntry?
}