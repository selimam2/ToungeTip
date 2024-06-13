package com.tonguetip

import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonguetip.ui.theme.TongueTipTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }
}
@Composable
fun MainScreen(
    viewModel: MainActivityViewModel = viewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)

    TongueTipTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column()
            {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    ListeningStatus()
                    Spacer(modifier = Modifier.height(100.dp))
                    LiveText(liveText = uiState.liveTextString)
                }
                SuggestionsButton()
                Spacer(modifier = Modifier.height(180.dp))
            }

        }
    }
}

@Composable
fun SuggestionsButton(modifier: Modifier = Modifier, viewModel: MainActivityViewModel = viewModel()) {
    Box(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {viewModel.buttonTest()},
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
            .fillMaxWidth().padding(20.dp),
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