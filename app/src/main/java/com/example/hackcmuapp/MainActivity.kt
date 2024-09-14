package com.example.hackcmuapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import com.example.hackcmuapp.ui.theme.HackCMUAppTheme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up the output directory
        outputDirectory = getOutputDirectory()

        // Check for camera permissions
        if (allPermissionsGranted()) {
            // Start the camera when the app loads
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        setContent {
            HackCMUAppTheme {
                var showCameraPreview by remember { mutableStateOf(true) }
                var showLeaderboard by remember { mutableStateOf(false) }
                var capturedPhotoFile by remember { mutableStateOf<File?>(null) }

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
                                    IconButton(onClick = {
                                        showLeaderboard = true
                                        showCameraPreview = false
                                    }) {
                                        Image(
                                            painter = painterResource(id = R.drawable.leaderboard),
                                            contentDescription = "Leaderboard Icon",
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    // Circular Button with Camera Icon in the center
                                    IconButton(
                                        onClick = {
                                            showCameraPreview = true
                                            showLeaderboard = false
                                        },
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
                                    IconButton(onClick = {
                                        showLeaderboard = false
                                        showCameraPreview = false
                                    }) {
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
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            showLeaderboard -> LeaderboardView()
                            showCameraPreview -> CameraPreviewView(onPhotoCaptured = { photoFile ->
                                capturedPhotoFile = photoFile
                            })
                            else -> {
                                // Show captured photo or a default image if none was captured
                                capturedPhotoFile?.let {
                                    Image(
                                        painter = rememberImagePainter(it),
                                        contentDescription = "Captured Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: Image(
                                    painter = painterResource(id = R.drawable.bg),
                                    contentDescription = "Default Background",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto(onPhotoCaptured: (File) -> Unit) {
        val photoFile = File(
            outputDirectory,
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")
                    onPhotoCaptured(photoFile)
                }
            }
        )
    }

    private fun uploadPhoto(photoFile: File, callback: (Boolean) -> Unit) {
        val url = "https://your-server.com/upload"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", photoFile.name,
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), photoFile)
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Upload failed", e)
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Upload successful")
                    callback(true)
                } else {
                    Log.e(TAG, "Upload failed with code: ${response.code}")
                    callback(false)
                }
            }
        })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    fun CameraPreviewView(onPhotoCaptured: (File) -> Unit) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

        AndroidView(factory = { previewViewContext ->
            val previewView = androidx.camera.view.PreviewView(previewViewContext)
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,  // Pass the lifecycle owner
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            previewView
        }, modifier = Modifier.fillMaxSize())

        // Capture photo on button click
        Button(onClick = {
            takePhoto { photoFile ->
                onPhotoCaptured(photoFile)
            }
        }) {
            Text(text = "Take Photo")
        }
    }

    @Composable
    fun LeaderboardView() {
        // Placeholder for Leaderboard content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Leaderboard", style = MaterialTheme.typography.headlineMedium)
            // You can add actual leaderboard content here
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
