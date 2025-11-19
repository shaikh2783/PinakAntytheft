package com.app.antitheft.ui.screens
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.antitheft.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAllPermissionGranted: (String) -> Unit,
    onActivateAdmin: () -> Unit,
    isDeviceAdminActive: Boolean
) {
    val state by viewModel.permissionState.collectAsState()

    // Launchers for standard runtime permissions
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissions() }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    // This effect runs once to check the initial permission state.
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Step 1: Handle Location Permission
            !state.hasLocation -> {
                PermissionStepCard(
                    title = "1. Location Access",
                    description = "We need your location to track your device for safety and to send alerts if it's lost.",
                    buttonText = "Allow Location",
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
            // Step 2: Handle Background Location
            !state.hasBackgroundLocation -> {
                PermissionStepCard(
                    title = "2. Background Location",
                    description = "Allow background location so we can find your device even when the app is closed.",
                    buttonText = "Allow Always",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                )
            }
            // Step 3: Handle Camera Permission
            !state.hasCamera -> {
                PermissionStepCard(
                    title = "3. Camera Access",
                    description = "We use the camera to take a photo of an intruder if they enter the wrong password.",
                    buttonText = "Allow Camera",
                    onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            // Step 4: All permissions are granted. Show the final dashboard.
            else -> {
                // This effect runs when all permissions are granted to start the background service.
                LaunchedEffect("all-granted-final") {
                    val uid = viewModel.userId
                    if (!viewModel.serviceStarted && !uid.isNullOrEmpty()) {
                        viewModel.serviceStarted = true
                        onAllPermissionGranted(uid)
                    }
                }

                // CHANGED: Pass the admin activation logic to the final dashboard content screen.
                RealDashboardContent(
                    onActivateAdmin = onActivateAdmin,
                    isDeviceAdminActive = isDeviceAdminActive
                )
            }
        }
    }
}

@Composable
fun RealDashboardContent(
    onActivateAdmin: () -> Unit,
    isDeviceAdminActive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Permissions Granted!",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "The final step is to activate the anti-theft service.",
                fontSize = 16.sp,
                color = Color(0xFFBBBBBB),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))

            // CHANGED: The button for activating Device Admin is now here.
            Button(
                onClick = onActivateAdmin,
                enabled = !isDeviceAdminActive // Disable the button if admin is already active
            ) {
                Text(
                    text = if (isDeviceAdminActive) "Anti-Theft Service is Active" else "Activate Anti-Theft Service",
                    fontSize = 16.sp
                )
            }
        }
    }
}

// CHANGED: This composable is now simplified and only handles one permission at a time.
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
        Text(
            description,
            color = Color(0xFFBBBBBB),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClick) {
            Text(buttonText, fontSize = 16.sp)
        }
    }
}
