package com.example.hackcmuapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
                // State to control which screen is shown, set to true by default to show bg.png
                var showLeaderboard by remember { mutableStateOf(true) }
                var showCamera by remember { mutableStateOf(false) }

                // Scaffold to hold the bottom bar and content
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
                                            painter = painterResource(id = R.drawable.leaderboard), // Using leaderboard.png
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
                                            painter = painterResource(id = R.drawable.camera), // Using camera.png
                                            contentDescription = "Camera Icon",
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    // Right Button with Dog Icon
                                    IconButton(onClick = { /* Your Dog Action */ }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.dog), // Using dog.png
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
                    // Box that holds the main content and respects padding from Scaffold
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (showCamera) {
                            // Display in-app camera preview
                            CameraPreviewView()
                        } else if (showLeaderboard) {
                            // Show background image by default
                            Image(
                                painter = painterResource(id = R.drawable.bg), // Using bg.png
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Default screen content (if toggled back)
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
