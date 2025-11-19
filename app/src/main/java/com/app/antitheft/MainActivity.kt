package com.app.antitheft

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.antitheft.Helper.datastore.DataStoreManager
import com.app.antitheft.receiver.MyDeviceAdminReceiver
import com.app.antitheft.service.FirebaseListenerService
import com.app.antitheft.ui.screens.DashboardScreen
import com.app.antitheft.viewmodel.DashboardViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Lazily initialize these properties as they are tied to the activity's context.
    private val devicePolicyManager: DevicePolicyManager by lazy {
        getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val adminComponent: ComponentName by lazy {
        ComponentName(this, MyDeviceAdminReceiver::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        val dataStore = DataStoreManager.getInstance(this)
        val viewModel: DashboardViewModel by viewModels()

        // Safely collect the user ID from DataStore only when the UI is visible.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStore.getUserId.collect { id ->
                    println("Stored User ID = $id")
                    if (!id.isNullOrEmpty()) {
                        viewModel.userId = id // Assign to ViewModel
                    }
                }
            }
        }

        setContent {
            // Pass the functions as parameters to your Composable screen.
            // This is a standard pattern for handling events from Compose UI.
            DashboardScreen(
                viewModel = viewModel,
                onAllPermissionGranted = { userId ->
                    startFirebaseService(userId)
                },
                // NEW: Pass a function to request admin permission.
                onActivateAdmin = {
                    requestDeviceAdmin()
                },
                // NEW: Pass the current admin status to the UI.
                isDeviceAdminActive = devicePolicyManager.isAdminActive(adminComponent)
            )
        }
    }

    /**
     * This function handles the logic for requesting Device Administrator permissions.
     * It should be called from a UI element, like a button click.
     */
    private fun requestDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            // If the app is not a device admin, ask the user to activate it.
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "This permission is required to detect wrong unlock attempts and enhance security."
                )
            }
            startActivity(intent)
        } else {
            // Inform the user that it's already active.
            Toast.makeText(this, "Device admin is already active.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFirebaseService(userId: String) {
        val intent = Intent(this, FirebaseListenerService::class.java).apply {
            putExtra("userId", userId)
        }
        ContextCompat.startForegroundService(this, intent)
    }
}
