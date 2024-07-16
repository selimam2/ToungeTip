package com.tonguetip

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonguetip.ui.theme.TongueTipTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import java.util.HashMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import java.io.IOException

class DetailedSuggestionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var suggestion = intent.getStringExtra("suggestion")
        setContent{
            TongueTipTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if(suggestion != null){
                        DisplaySuggestion(suggestion)
                    }
                }
            }
        }
    }
}

@Composable
fun DisplaySuggestion(suggestion: String, viewModel: DetailedSuggestionActivityViewModel = viewModel{DetailedSuggestionActivityViewModel(suggestion)}) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)
    val dictionaryEntries = uiState.words
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center // Align text to the center horizontally
        )
        {
            Text(
                text = "\"$suggestion\"",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (!dictionaryEntries.isNullOrEmpty())
        {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f))
            {
                DisplayDictionaryEntry(dictionaryEntries[0])
            }
            WordPronunciationCard(dictionaryEntries[0].pronunciationURL)
        }

    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun DisplayDictionaryEntry(entry: DictionaryEntry){
    Column(){
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.CenterStart // Align text to the center horizontally
        )
        {
            Text(
                text = "Definitions:",
                textAlign = TextAlign.Left,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        LazyColumn {
            entry.meanings!!.forEach { (partOfSpeech, definitionsForPartOfSpeech) ->
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                    )
                    {
                        Text(
                            text = "${partOfSpeech.value}:",
                            textAlign = TextAlign.Left,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                itemsIndexed(definitionsForPartOfSpeech) { index, definition ->
                    DefinitionCard(index,definition.definition,definition.example,definition.synonyms,definition.antonyms)
                    if(index != definitionsForPartOfSpeech.count()-1)
                    Spacer(Modifier.size(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DefinitionCard(index: Int, definition: String, example: String ?= null, synonyms:List<String>?=null, antonyms:List<String>?=null){
    val num = index + 1
    var expandedState by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expandedState) 0f else -90f
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            ),
        onClick = {
            expandedState = !expandedState
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$num. \"$definition\"",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Left,
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(10f)
                )
                IconButton(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.2f)
                        .rotate(rotationState),
                    onClick = {
                        expandedState = !expandedState
                    }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown arrow"
                    )
                }
            }
            if (expandedState) {
                if(example != null)
                {
                    Text(
                        text = "Ex: \"$example\"",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .padding(8.dp),
                    )
                }
                if(!synonyms.isNullOrEmpty())
                {
                    Text(
                        text = "Synonyms: \"${synonyms}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .padding(8.dp),
                    )
                }
                if(!antonyms.isNullOrEmpty())
                {
                    Text(
                        text = "Antonyms: \"$antonyms\"",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .padding(8.dp),
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPronunciationCard(url: String?){
    var playText = "Play Pronunciation"
    var playIcon = Icons.Default.PlayArrow
    if(url == null){
        playText = "Cannot Play Pronunciation"
        playIcon = Icons.Default.Warning
    }
    val context = LocalContext.current
    ElevatedCard(elevation = CardDefaults.cardElevation(
        defaultElevation = 5.dp),
        onClick = {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            try {
                mediaPlayer.setDataSource(context, Uri.parse(url))
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { mp ->
                    mp.start()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Left,
                modifier = Modifier
                    .padding(16.dp)
                    .weight(10f)
            )
            Icon(
                imageVector = playIcon,
                contentDescription = "Play Audio Arrow",
                modifier = Modifier
                    .weight(2f)
                    .alpha(1f),
            )
        }
    }
}
@Preview
@Composable
fun DisplaySuggestionPreview() {
    Surface(color = MaterialTheme.colorScheme.background) {
        DisplaySuggestion("Hello")
    }
}