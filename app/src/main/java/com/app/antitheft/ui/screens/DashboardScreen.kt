package com.app.antitheft.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.antitheft.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAllPermissionGranted: (String) -> Unit
) {
    val state by viewModel.permissionState.collectAsState()


    // Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // After user decision
        viewModel.refreshPermissions()
    }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        when {
            !state.hasLocation -> {
                PermissionStepCard(
                    title = "Location access",
                    description = "We need your location to track your device safety and send alerts.",
                    buttonText = "Allow location",
                    onClick = {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            !state.hasBackgroundLocation -> {
                PermissionStepCard(
                    title = "Background location",
                    description = "Allow background location so we can update your location even when the app is not open.",
                    buttonText = "Allow in settings",
                    onClick = {
                        // On Android 11+ you typically open app settings for this,
                        // or request ACCESS_BACKGROUND_LOCATION if allowed.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                )
            }

            !state.hasCamera -> {
                PermissionStepCard(
                    title = "Camera access",
                    description = "We use the camera for security features and scanning.",
                    buttonText = "Allow camera",
                    onClick = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }

            else ->{
                LaunchedEffect("all-granted-final") {
                    val uid = viewModel.userId
                    if (!viewModel.serviceStarted && uid != null) {
                        viewModel.serviceStarted = true
                        onAllPermissionGranted(uid)
                    }
                }

                RealDashboardContent()
            }
        }
    }
}


@Composable
fun RealDashboardContent() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10)),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            androidx.compose.material3.Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "All permissions granted!",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "You can now use all security services.",
                fontSize = 16.sp,
                color = Color(0xFFBBBBBB),
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun PermissionStepCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(description, color = Color.White, fontSize = 14.sp)

        Spacer(Modifier.height(24.dp))

        Button(onClick = onClick) {
            Text(buttonText)
        }
    }
}
