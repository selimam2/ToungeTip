package com.tonguetip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tonguetip.ui.theme.TongueTipTheme


class LaunchedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            var canRecord by remember {
                mutableStateOf(false)
            }

            val context = LocalContext.current

            canRecord = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            TongueTipTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally)
                    {
                        if (canRecord)
                        {
                            Button(
                                onClick = {context.startActivity(Intent(context, MainActivity::class.java))},
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            )
                            {

                                Text(text = "Start Listening")
                            }
                        }
                        else{
                            val recordAudioLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission(),
                                onResult = { isGranted ->
                                    canRecord = isGranted
                                }
                            )

                            Button(
                                onClick = {recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)},
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            )
                            {

                                Text(text = "Need Recording Permissions")
                            }
                        }
                    }

                }
            }
        }
    }
}
