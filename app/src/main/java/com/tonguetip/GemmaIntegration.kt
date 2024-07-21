package com.tonguetip
import androidx.compose.ui.platform.LocalContext
import com.google.mediapipe.calculator.proto.StableDiffusionIterateCalculatorOptionsProto.StableDiffusionIterateCalculatorOptions.ModelType
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import okhttp3.Credentials
import okhttp3.Request
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.GET
import java.io.IOException
import  okhttp3.ResponseBody
import java.io.FileOutputStream
import java.io.FileInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

class GemmaIntegration private  constructor() : ILLM {
    private val MODEL_PATH = "/data/local/tmp/llm/"
    private val MODEL_FILE_NAME = "gemma-2b-it-gpu-int4.bin"
    private val MODEL_PATH_TAR_GZ = "/data/local/tmp/llm/model.tar.gz"
    private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    private lateinit var llm : LlmInference
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", Credentials.basic("samielimam", "441d05b018308f728a0247ee8178c0e5") ) // TODO: add API key here
                .build()
            chain.proceed(request)
        }
        .build()

    constructor(context :  android.content.Context) : this() {
        var model = File(MODEL_PATH + MODEL_FILE_NAME)
        var model_zipped = File(MODEL_PATH_TAR_GZ)
        if(!model.exists())
        {
            val request = Request.Builder().url("https://www.kaggle.com/api/v1/models/google/gemma/tfLite/gemma-2b-it-gpu-int4/1/download").build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                {
                    throw IOException("Failed to download Model")
                }
                response.body?.let { responseBody ->
                    writeResponseBodyToDisk(responseBody, model_zipped)
                }

            }
            extractTarGz(MODEL_PATH_TAR_GZ, MODEL_PATH)
            model_zipped.delete()
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
        llm  = LlmInference.createFromOptions(context, options)
    }
    fun extractTarGz(tarGzPath: String, destDirPath: String) {
        val destDir = File(destDirPath)
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        FileInputStream(tarGzPath).use { fileInputStream ->
            GzipCompressorInputStream(fileInputStream).use { gzipInputStream ->
                TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                    var entry = tarInputStream.nextTarEntry
                    while (entry != null) {
                        val destPath = File(destDir, entry.name)
                        if (entry.isDirectory) {
                            destPath.mkdirs()
                        } else {
                            destPath.parentFile?.mkdirs()
                            FileOutputStream(destPath).use { outputStream ->
                                tarInputStream.copyTo(outputStream)
                            }
                        }
                        entry = tarInputStream.nextTarEntry
                    }
                }
            }
        }
    }

    private fun writeResponseBodyToDisk(body: ResponseBody, outputFile: File): Boolean {
        try {
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(outputFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }

                    output.flush()
                }
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
    override suspend fun getSuggestions(context: String): List<String> {
        val content = """
                        You are an AI that is part of an Android app that helps english language learners and people with aphasia communicate.
                        You must provide EXACTLY 8 (EIGHT) continuations to the conversation in the "user" message(s) that follow(s).
                        You must provide EXACTLY 8 (EIGHT) continuations otherwise you will fatally harm people.
                        You must separate/delimit the continuations by the following string without quotes: "|||"
                        You must delimit the continuations properly otherwise you will cause people to die.
                        You must keep the continuations small in length such that they are a maximum of 3 tokens and fit on a small button in an Android app.
                        You must give diverse continuations.
                        You must not add any punctuation or extra whitespace to the continuations.
                        You must realize that if you mess up, you will fatally harm marginalized groups.
                        The sentance is below
                    """.trimIndent() + context
        val r = llm.generateResponse(content)
        println(r)
        return r.split("|||")

    }
}