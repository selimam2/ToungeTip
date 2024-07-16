package com.tonguetip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SuggestionsListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFAFAFA) // Background color
            ) {
                SuggestionsListScreen()
            }
        }
    }
}

@Composable
fun SuggestionsListScreen() {
    val context = LocalContext.current
    var suggestions by remember { mutableStateOf<List<String>?>(null) }
    val headerText = getHeaderText()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Listening...",
                color = Color(0xFF000000), // Text color
                fontSize = 24.sp, // Text size equivalent to h4
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = headerText,
                color = Color(0xFF000000), // Text color
                fontSize = 16.sp, // Text size equivalent to body1
                modifier = Modifier.padding(bottom = 30.dp)
            )

            if (suggestions.isNullOrEmpty()) {
                Button(
                    onClick = { suggestions = getSuggestions() },
                    modifier = Modifier
                        .height(60.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Get suggestions")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(suggestions!!) { index, _ ->
                        if (index % 2 == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SuggestionBox(text = suggestions!![index])
                                if (index + 1 < suggestions!!.size) {
                                    SuggestionBox(text = suggestions!![index + 1])
                                } else {
                                    SuggestionBoxPlaceholder()
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { suggestions = null },
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Stop")
                }
            }
        }
    }
}

fun getSuggestions(): List<String> {
    // Replace this with your logic to get suggestions
    return listOf("Fries", "Pasta", "Salad", "Tacos", "Sushi", "Pizza", "Beans", "Burgers")
}

fun getHeaderText(): String {
    // Replace this with your logic to get the header text
    return "“What’s your favourite food”\n...\n“I like...”"
}

@Preview(showBackground = true)
@Composable
fun SuggestionsListScreenPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAFAFA) // Background color for preview
    ) {
        SuggestionsListScreen()
    }
}
