package com.tonguetip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonguetip.DatabaseHandler.Companion.getContextForSuggestion
import com.tonguetip.ui.theme.TongueTipTheme

class UserDictionaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel:UserDictionaryActivityViewModel by viewModels()
        lifecycle.addObserver(viewModel)
        setContent{
            TongueTipTheme {
                HamburgerMenu(this, "Your Dictionary")
                {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize(),) {
                        DisplayDictionary(this@UserDictionaryActivity)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        DatabaseHandler.closeDatabase()
        super.onDestroy()
    }
}

@Composable
fun DisplayDictionary(context: Context, viewModel: UserDictionaryActivityViewModel = viewModel()){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(10.dp)
    ) {
        OutlinedCard(modifier = Modifier
            .padding(5.dp)
            .fillMaxSize()){
            val dict = uiState.dictionary
            if(!dict.isNullOrEmpty())
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    contentAlignment = Alignment.Center // Align text to the center horizontally
                )
                {
                    LazyColumn(verticalArrangement = Arrangement.SpaceEvenly) {
                        dict.forEach{entry ->
                            item{
                                dictItem(entry.key, context, entry.value)
                            }
                        }
                    }
                }
            }
            else{
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)){
                        Text(text = "You have no forgotten words yet!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,)
                }
            }
        }

    }
}

@Composable
fun dictItem(dictEntry: String, context: Context, suggestionContexts: MutableSet<String>){
    Card(
        modifier = Modifier
            .padding(10.dp)
            ,
        shape = RoundedCornerShape(10.dp),
        onClick = {
            val intent = Intent(context, DetailedSuggestionActivity::class.java)
            val strContextList = ArrayList(suggestionContexts)
            var strContext = strContextList[0]
            if(strContextList.isNullOrEmpty()){strContext = ""}

            intent.putExtra("suggestion", dictEntry)
            intent.putExtra("suggestionContext", strContext)
            intent.putExtra("hideCorrectCard", true)
            intent.putStringArrayListExtra("strContextList", strContextList)
            context.startActivity(intent)
        }
    ){
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                text = dictEntry,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
    }
}
