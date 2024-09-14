package com.example.hackcmuapp

import android.util.Log


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.hackcmuapp.ui.theme.HackCMUAppTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor() // Executor for camera tasks
        setContent {
            HackCMUAppTheme {
                var showLeaderboard by remember { mutableStateOf(false) }
                var showCamera by remember { mutableStateOf(false) }

                // List of random names for the leaderboard
                val randomNames = listOf(
                    "John Doe", "Jane Smith", "Michael Johnson", "Emily Davis",
                    "Chris Brown", "Jessica Wilson", "David Lee", "Sarah Miller",
                    "Daniel Garcia", "Sophia Martinez"
                )

                Scaffold(
                    bottomBar = {
                        BottomAppBar(
                            modifier = Modifier.height(80.dp),
                            content = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Button with Leaderboard Icon
                                    IconButton(onClick = { showLeaderboard = true; showCamera = false }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.leaderboard),
                                            contentDescription = "Leaderboard Icon",
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    // Circular Button with Camera Icon in the center
                                    IconButton(
                                        onClick = { showCamera = true; showLeaderboard = false },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .size(60.dp)
                                            .padding(8.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.camera),
                                            contentDescription = "Camera Icon",
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    // Right Button with Dog Icon
                                    IconButton(onClick = { /* Your Dog Action */ }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.dog),
                                            contentDescription = "Dog Icon",
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when {
                            showCamera -> {
                                CameraPreviewView()
                            }
                            showLeaderboard -> {
                                LeaderboardView(randomNames)
                            }
                            else -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "Welcome Screen", style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraPreviewView() {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { previewViewContext ->
            val previewView = androidx.camera.view.PreviewView(previewViewContext)

            val cameraProvider = cameraProviderFuture.get()

            // Set up Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    context as ComponentActivity,
                    cameraSelector,
                    preview
                )

            } catch (exc: Exception) {
                Log.e("CameraPreview", "Use case binding failed", exc)
            }

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LeaderboardView(names: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Leaderboard",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // LazyColumn to display random names
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(names) { name ->
                LeaderboardItem(name)
            }
        }
    }
}

@Composable
fun LeaderboardItem(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Text(
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}
