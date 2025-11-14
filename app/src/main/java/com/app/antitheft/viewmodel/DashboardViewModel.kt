package com.app.antitheft.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import com.app.antitheft.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionUiState(
    val hasLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val hasCamera: Boolean = false
)

class DashboardViewModel(
    private val app: Application
) : AndroidViewModel(app) {
    var userId: String? = null
    var serviceStarted: Boolean = false


    private val _permissionState = MutableStateFlow(PermissionUiState())
    val permissionState = _permissionState.asStateFlow()
    private val locationRepo = LocationRepository(app)

    fun onAllPermissionsGranted() {
        val prefs = app.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", null) ?: return

        // Attach Firebase listener
        locationRepo.startListening(userId)
    }
    fun refreshPermissions() {
        val context = app.applicationContext
        val hasLoc = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCam = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasBgLoc = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _permissionState.value = PermissionUiState(
            hasLocation = hasLoc,
            hasBackgroundLocation = hasBgLoc,
            hasCamera = hasCam
        )
    }
}
