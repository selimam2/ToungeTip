package com.tonguetip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonguetip.ui.theme.TongueTipTheme

class UserMetricsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            TongueTipTheme {
                HamburgerMenu(this, "Your Metrics")
                {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize(),) {
                        DisplayMetrics()
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
fun DisplayMetrics(viewModel: UserMetricsActivityViewModel = viewModel()){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(10.dp)
    ) {
        OutlinedCard(modifier = Modifier.padding(5.dp).fillMaxSize()){
            val metrics = uiState.metrics
            if(!metrics.isNullOrEmpty())
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    contentAlignment = Alignment.Center // Align text to the center horizontally
                )
                {
                    LazyColumn(verticalArrangement = Arrangement.SpaceEvenly) {
                        itemsIndexed(metrics) { _, metric ->
                            MetricsCard(metric)
                        }
                    }
                }
            }
            else{
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().weight(1f)) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(64.dp)
                    )
                }
            }
        }

    }
}

@Composable
fun MetricsCard(metric: Metric){
    Card(
        modifier = Modifier
            .padding(20.dp)
            .width(280.dp), shape = RoundedCornerShape(10.dp)){

        Column {
            ElevatedCard(elevation = CardDefaults.cardElevation(
                defaultElevation = 5.dp), shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    text = metric.getTitle(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                text = metric.getBody(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview
@Composable
fun PreviewDisplayMetrics(){
    DisplayMetrics()
}