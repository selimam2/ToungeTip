package com.tonguetip

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HamburgerMenu(context: Context, title:String = "", modifier: Modifier = Modifier,
                  content: @Composable ColumnScope.() -> Unit
){
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(drawerState = drawerState,drawerContent = {
        ElevatedCard(modifier = Modifier
            .fillMaxHeight()) {
            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = androidx.compose.ui.Modifier.size(40.dp))

                Button(modifier = Modifier.padding(5.dp),shape = CircleShape, contentPadding = PaddingValues(0.dp), onClick = {
                    val intent = Intent(context, SettingsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                    scope.launch {
                        drawerState.apply {
                            close()
                        }
                    }

                }
                , colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Filled.Settings, contentDescription = "",tint = MaterialTheme.colorScheme.onTertiary)
                }
                HorizontalDivider(thickness = 3.dp, modifier = Modifier.width(100.dp))
                Button(modifier = Modifier.padding(5.dp),shape = RoundedCornerShape(40f), onClick = {
                    val intent = Intent(context, LaunchedActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                    scope.launch {
                        drawerState.apply {
                            close()
                        }
                    }
                }) {
                    Text(text = "Home")
                }
                Button(modifier = Modifier.padding(5.dp),shape = RoundedCornerShape(40f), onClick = {
                    val intent = Intent(context, QuizActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                    scope.launch {
                        drawerState.apply {
                            close()
                        }
                    }
                }) {
                    Text(text = "Quiz")
                }
                Button(modifier = Modifier.padding(5.dp),shape = RoundedCornerShape(40f), onClick = {
                    val intent = Intent(context, UserDictionaryActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                    scope.launch {
                        drawerState.apply {
                            close()
                        }
                    }
                }) {
                    Text(text = "Dictionary")
                }
                Button(modifier = Modifier.padding(5.dp),shape = RoundedCornerShape(40f), onClick = {
                    val intent = Intent(context, UserMetricsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                    scope.launch {
                        drawerState.apply {
                            close()
                        }
                    }
                }) {
                    Text(text = "Metrics")
                }
            }
        }


    }){
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            modifier = Modifier,
                            text = title,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(modifier = Modifier, onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Hamburger Icon"
                            )
                        }
                    }
                )
                
            }
        ){paddingValues ->
            Column(Modifier.padding(paddingValues), content = content)
        }
    }
}