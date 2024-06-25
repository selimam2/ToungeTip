package com.tonguetip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import java.util.Locale

class VoiceRecognizer(context: Context) {
    private var speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
    private var recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    private var shouldContinueListening = true;
    private var recognizedText = ""
    private var partialText = ""
    var currtext = ""
    private lateinit var update : (str: String) -> Unit
    private val mRecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(p0: Bundle?) {
            Log.i("speech", "readyforspeech")
        }

        override fun onBeginningOfSpeech() {
            //TODO("Not yet implemented")
            Log.i("speech", "beg")
        }

        override fun onRmsChanged(p0: Float) {
            //TODO("Not yet implemented")
        }

        override fun onBufferReceived(p0: ByteArray?) {
            //TODO("Not yet implemented")
        }

        override fun onEndOfSpeech() {
            Log.i("speech", "end")
            if(shouldContinueListening){
                resumeListening()
            }
        }

        override fun onError(p0: Int) {
            //TODO("Not yet implemented")
        }

        override fun onResults(p0: Bundle?) {
            Log.i("speech", "res")
            val matches: ArrayList<String>? = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            var text = "";
            for (result in matches!!) text += """$result """.trimIndent();
            partialText = "";
            recognizedText += text;
            currtext = recognizedText

            if(::update.isInitialized){
                update(currtext)
            }

            Log.i("SpeechText", recognizedText)
            if(shouldContinueListening){
                resumeListening()
            }

        }

        override fun onPartialResults(p0: Bundle?) {
            val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            var text = "";
            for (result in matches!!) text += """$result """.trimIndent();
            partialText = text;
            currtext = recognizedText + partialText
            if(::update.isInitialized){
                update(currtext)
            }
        }

        override fun onEvent(p0: Int, p1: Bundle?) {
            //TODO("Not yet implemented")
        }
    }

    init{
        //configure intent
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(mRecognitionListener);

    }

    public fun startListening(){
        recognizedText = ""
        partialText = ""
        currtext = ""
        speechRecognizer.startListening(recognizerIntent);
        shouldContinueListening = true;

    }
    private fun resumeListening(){
        speechRecognizer.startListening(recognizerIntent);
        shouldContinueListening = true;
    }
    public fun stopListening(){
        shouldContinueListening = false
        speechRecognizer.stopListening();
    }

    public fun initUpdateFn(fn: (input: String) -> Unit) {
        update = fn;
        return
    }

}