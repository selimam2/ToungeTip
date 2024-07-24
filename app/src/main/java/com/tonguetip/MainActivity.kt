package com.tonguetip

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.size
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonguetip.ui.theme.TongueTipTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

var recognizer: VoiceRecognizer? = null;

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        recognizer = VoiceRecognizer(this)
        recognizer!!.startListening()
        super.onCreate(savedInstanceState)

        // Close activity on back button
        val callback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(
            this, // LifecycleOwner
            callback
        )

        enableEdgeToEdge()
        setContent {
            TongueTipTheme {
                HamburgerMenu(this)
                {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(recognizer != null){
            recognizer!!.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        if(recognizer != null){
            recognizer!!.stopListening()
        }
    }
}
@Composable
fun MainScreen(
    viewModel: MainActivityViewModel = viewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)
    val suggestions = uiState.suggestions
    recognizer!!.initUpdateFn(viewModel::updateLiveText)
    TongueTipTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column()
            {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    ListeningStatus()
                    Spacer(modifier = Modifier.height(100.dp))
                    LiveText(liveText = uiState.liveTextString)
                }
                if ( suggestions.isNullOrEmpty()) {
                    SuggestionsButton()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(suggestions) { index, _ ->
                            if (index % 2 == 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    SuggestionBox(text = suggestions[index])
                                    if (index + 1 < suggestions.size) {
                                        SuggestionBox(text = suggestions[index + 1])
                                    } else {
                                        //SuggestionBoxPlaceholder()
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.suggestionReset()
                            recognizer!!.startListening()},
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Stop")
                    }
                }
                Spacer(modifier = Modifier.height(180.dp))
            }

        }
    }
}

@Composable
fun SuggestionsButton(modifier: Modifier = Modifier, viewModel: MainActivityViewModel = viewModel()) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                       if(viewModel.buttonTest(context))
                       {
                           recognizer!!.stopListening()
                       }
                 },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(250.dp)
        ) {
            Text(text = "Get Suggestions",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LiveText(liveText: String, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentAlignment = Alignment.Center // Align text to the center horizontally
    )
    {
    Text(
        text = liveText,
        style = MaterialTheme.typography.titleLarge,
        overflow = TextOverflow.Ellipsis,
        maxLines = 4

    )
    }
}

@Composable
fun ListeningStatus(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center // Align text to the center horizontally

    ) {
        Text(text = "Listening ...",
            color = Color.Red)
    }
}

@Composable
fun SuggestionBox(text: String, viewModel: MainActivityViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .width(150.dp)
            .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(8.dp))
            .height(80.dp)
            .clickable(onClick = {
                var intent = Intent(context, DetailedSuggestionActivity::class.java)
                intent.putExtra("suggestion", text)
                intent.putExtra("suggestionContext", uiState.liveTextString)
                context.startActivity(intent)
            }),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color(0xFF000000), fontSize = 16.sp)
    }
}

@Composable
fun SuggestionBoxPlaceholder() {
    Box(
        modifier = Modifier
            .padding(5.dp)
    )
}