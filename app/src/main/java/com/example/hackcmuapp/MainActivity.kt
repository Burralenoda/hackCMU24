package com.example.hackcmuapp
import com.example.hackcmuapp.ui.theme.HackCMUAppTheme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HackCMUAppTheme {
                // Main content area with a bottom navigation bar
                Scaffold(
                    bottomBar = {
                        BottomAppBar {
                            Button(
                                onClick = { /* Handle button 1 click */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Button 1")
                            }
                            Button(
                                onClick = { /* Handle button 2 click */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Button 2")
                            }
                        }
                    }
                ) { innerPadding ->
                    // Main content goes here
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Main Content", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}