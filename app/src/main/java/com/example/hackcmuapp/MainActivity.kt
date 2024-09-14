package com.example.hackcmuapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Base64
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private lateinit var imageCapture: ImageCapture
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize the fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the output directory
        outputDirectory = getOutputDirectory()

        // Check for camera and location permissions
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
                var showMoney by remember { mutableStateOf(false) }
                var selectedButton by remember { mutableStateOf("camera") }

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
                                    IconButton(
                                        onClick = {
                                            showLeaderboard = true
                                            showCameraPreview = false
                                            showMoney = false
                                            selectedButton = "leaderboard"
                                        },
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                if (selectedButton == "leaderboard") Color.Black else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .padding(8.dp)
                                    ) {
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
                                            showMoney = false
                                            takeAndUploadPhotoWithLocation() // Updated function
                                            selectedButton = "camera"
                                        },
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                if (selectedButton == "camera") Color.Black else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.camera),
                                            contentDescription = "Camera Icon",
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    // Right Button with Dog Icon
                                    IconButton(
                                        onClick = {
                                            showLeaderboard = false
                                            showCameraPreview = false
                                            showMoney = false
                                            selectedButton = "dog"
                                        },
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                if (selectedButton == "dog") Color.Black else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.dog),
                                            contentDescription = "Dog Icon",
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    // New Right Button with Dollar Icon
                                    IconButton(
                                        onClick = {
                                            showMoney = true
                                            showLeaderboard = false
                                            showCameraPreview = false
                                            selectedButton = "money"
                                        },
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                if (selectedButton == "money") Color.Black else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.dollar),
                                            contentDescription = "Dollar Icon",
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
                            showMoney -> {
                                // Show "MONEY" when the dollar icon is clicked
                                Text(
                                    text = "MONEY",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.Black
                                )
                            }
                            selectedButton == "dog" -> ProfileView() // Display the profile view when dog icon is clicked
                            else -> {
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

    @SuppressLint("MissingPermission")
    private fun takeAndUploadPhotoWithLocation() {
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
                    getCurrentLocation { location ->
                        if (location != null) {
                            val base64Image = encodeImageToBase64(photoFile)

                            uploadPhotoWithLocation(
                                base64Image,
                                location.latitude,
                                location.longitude
                            ) { success ->
                                runOnUiThread {
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (success) "Photo and location uploaded successfully!"
                                        else "Upload failed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to get location",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(onLocationRetrieved: (Location?) -> Unit) {
        val locationTask: Task<Location> = fusedLocationClient.lastLocation
        locationTask.addOnSuccessListener { location: Location? ->
            onLocationRetrieved(location)
        }
    }

    private fun encodeImageToBase64(photoFile: File): String {
        val inputStream = FileInputStream(photoFile)
        val bytes = inputStream.readBytes()
        inputStream.close()
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun uploadPhotoWithLocation(
        base64Image: String,
        latitude: Double,
        longitude: Double,
        callback: (Boolean) -> Unit
    ) {
        val url = "https://f187-128-237-82-8.ngrok-free.app/upload"

        // Create JSON body with the Base64 image and location data
        val jsonBody = """
            {
                "image": "$base64Image",
                "latitude": $latitude,
                "longitude": $longitude
            }
        """.trimIndent()

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

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

    @Composable
    fun ProfileView() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display profile image
            Image(
                painter = painterResource(id = R.drawable.dog),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display name
            Text(
                text = "Anurag Kapila",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Display verified status
            Text(
                text = "Verified",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
