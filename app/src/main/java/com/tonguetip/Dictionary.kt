package com.tonguetip

import com.google.gson.annotations.SerializedName

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import retrofit2.http.GET
import retrofit2.http.Path


interface DictionaryAPI {
    @GET("v2/entries/en/{word}")
    fun getEntry(@Path("word") word: String): Call<List<SerialDictionaryEntry>>
}

data class SerialPhonetics(
    @SerializedName("text")
    val text: String,
    @SerializedName("audio")
    val audio: String
)

data class SerialDefinition(
    @SerializedName("definition")
    var definition: String,
    @SerializedName("example")
    var example: String,
    @SerializedName("synonyms")
    var synonyms: List<String>,
    @SerializedName("antonyms")
    var antonyms: List<String>,
)

data class SerialMeaning(
    @SerializedName("partOfSpeech")
    val partOfSpeech: String,
    @SerializedName("definitions")
    var definitions: List<SerialDefinition>,
)

data class SerialDictionaryEntry(
    @SerializedName("word")
    val word: String,
    @SerializedName("phonetics")
    val phonetics: List<SerialPhonetics>,
    @SerializedName("meanings")
    val meanings: List<SerialMeaning>

)
class Dictionary:DictionaryInterface {
    private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.dictionaryapi.dev/api/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(DictionaryAPI::class.java)

    override suspend fun getDictionaryEntry(word: String): DictionaryEntry? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getEntry(word).awaitResponse()
                if (response.isSuccessful) {
                    val entry = response.body()!![0]

                    var pronunciationURL:String ?= null
                    for(phonetic in entry.phonetics){
                        if(phonetic.audio != ""){
                            pronunciationURL = phonetic.audio
                            break
                        }
                    }
                    var mainMeaning:Definition? = null
                    if(entry.meanings.isNotEmpty() && entry.meanings[0].definitions.isNotEmpty())
                    {
                        val serialDef = entry.meanings[0].definitions[0]
                        mainMeaning = Definition(serialDef.definition,serialDef.example,serialDef.synonyms,serialDef.antonyms)
                    }
                    val meanings = LinkedHashMap<PartOfSpeech, List<Definition>>()
                    for(meaning in entry.meanings){
                        val definitions = mutableListOf<Definition>()
                        for (definition in meaning.definitions){
                            definitions.add(Definition(definition.definition,definition.example,definition.synonyms,definition.antonyms))
                        }
                        val pos = PartOfSpeech.fromString(meaning.partOfSpeech)
                        meanings[pos] =
                            if (meanings[pos] != null) meanings[pos]!!.plus(definitions) else definitions
                    }
                    val returnEntry = DictionaryEntry(word,pronunciationURL,meanings, mainMeaning)
                    returnEntry
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}