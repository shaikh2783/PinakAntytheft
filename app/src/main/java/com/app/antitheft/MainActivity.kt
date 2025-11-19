package com.app.antitheft
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.antitheft.Helper.datastore.DataStoreManager
import com.app.antitheft.service.FirebaseListenerService
import com.app.antitheft.ui.screens.DashboardScreen
import com.app.antitheft.viewmodel.DashboardViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlin.getValue
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        val dataStore = DataStoreManager.getInstance(this)
        val viewModel: DashboardViewModel by viewModels()
        lifecycleScope.launch {
            dataStore.getUserId.collect { id ->
                println("Stored User ID = $id")

                if (id != null) {
                    viewModel.userId = id   // ✅ IMPORTANT: assign to ViewModel
                }
            }
        }


        setContent {
            DashboardScreen(
                viewModel = viewModel,
                onAllPermissionGranted = { userId ->
                    startFirebaseService(userId)
                }
            )
        }
    }
    fun startFirebaseService(userId: String) {
        val intent = Intent(this, FirebaseListenerService::class.java)
        intent.putExtra("userId", userId)
        ContextCompat.startForegroundService(this, intent)
    }


}

