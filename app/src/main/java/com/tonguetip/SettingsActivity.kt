package com.tonguetip

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tonguetip.ui.theme.TongueTipTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {

            val context = LocalContext.current
            val sharedPreference =  this.getPreferences(Context.MODE_PRIVATE)

            TongueTipTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShowSettings(sharedPreference);
                }
            }
        }
    }
}
@Composable
fun ShowSettings(sharedPreferences: SharedPreferences){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.Center // Align text to the center horizontally
        )
        {
            Text(
                text = "Settings",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            contentAlignment = Alignment.Center // Align text to the center horizontally
        )
        {
            Column {
                // First in map is the default, maps from the option name to the human readable name
                // Name refers to the setting name, which you can use getString(name,default_value) to get
                SettingsCardDropdown("LLMOption", mapOf("ChatGPT" to "ChatGPT (Online)", "Gemma" to "Gemma (Offline)"),sharedPreferences)
                SettingsCardDropdown("NativeLanguage", mapOf("English" to "English (Eng)", "French" to "French (Fr)",
                    "Russian" to "Russian (Ru)"),sharedPreferences)
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCardDropdown(name:String,settingsOptions: Map<String,String>, sharedPreferences: SharedPreferences){
    var expandedState by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(settingsOptions[sharedPreferences.getString(name,settingsOptions.keys.elementAt(0))]) }
    ElevatedCard(
        modifier = Modifier
            .padding(20.dp)
            .width(280.dp), shape = RoundedCornerShape(10.dp)
    ) {
        Column(){
            Text(
                text = name,
                textAlign = TextAlign.Left,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(5.dp)
            )
            ExposedDropdownMenuBox(expanded = expandedState,
                onExpandedChange  = { expandedState = !expandedState}) {
                TextField(
                    value = selectedText!!,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedState,
                    onDismissRequest = { expandedState = false }
                ) {
                    settingsOptions.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(text = item.value) },
                            onClick = {
                                selectedText = item.value
                                expandedState = false
                                with(sharedPreferences.edit()){
                                    putString(name,item.key)
                                    apply()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
