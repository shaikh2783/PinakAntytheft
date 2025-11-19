package com.app.antitheft.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.app.antitheft.Helper.datastore.DataStoreManager
import com.app.antitheft.worker.SecurityWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
// FIXED: Corrected the import for launch
import kotlinx.coroutines.launch

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    // ADDED: It's good practice to have these methods for logging and debugging.
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Device Admin: Enabled", Toast.LENGTH_SHORT).show()
        Log.d("DeviceAdmin", "Device admin has been enabled.")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Device Admin: Disabled", Toast.LENGTH_SHORT).show()
        Log.d("DeviceAdmin", "Device admin has been disabled.")
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val failedAttempts = dpm.currentFailedPasswordAttempts

        Log.d("DeviceAdmin", "Wrong pattern entered. Total failed attempts: $failedAttempts")
        Toast.makeText(context, "Wrong Pattern! Attempts: $failedAttempts", Toast.LENGTH_SHORT).show()

        if (failedAttempts >= 2) {
            Log.w("DeviceAdmin", "Suspicious activity! Starting async work to enqueue worker.")

            // Use goAsync() to perform asynchronous work in a BroadcastReceiver
            val pendingResult: PendingResult = goAsync()
            val coroutineScope = CoroutineScope(Dispatchers.IO)

            coroutineScope.launch {
                try {
                    // Fetch the user ID asynchronously from DataStore
                    val dataStore = DataStoreManager.getInstance(context)
                    // .first() gets the first emitted value from the Flow
                    val userId = dataStore.getUserId.first()

                    // IMPROVED: Check for both null and empty string to be safer.
                    if (!userId.isNullOrEmpty()) {
                        Log.d("DeviceAdmin", "Found User ID from DataStore: $userId. Enqueuing work.")
                        enqueueSecurityWork(context, userId)
                    } else {
                        Log.e("DeviceAdmin", "Could not find a valid User ID in DataStore. Aborting worker.")
                    }
                } catch (e: Exception) {
                    Log.e("DeviceAdmin", "Error fetching User ID from DataStore.", e)
                } finally {
                    // You must call finish() when your asynchronous work is done.
                    Log.d("DeviceAdmin", "Async work finished, calling pendingResult.finish().")
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordSucceeded(context, intent, user)
        Log.d("DeviceAdmin", "Password attempt succeeded.")
        // On successful login, cancel any pending security tasks
        WorkManager.getInstance(context).cancelAllWorkByTag(SecurityWorker.TAG)
        Log.d("DeviceAdmin", "Pending security work cancelled.")
    }

    // This function now accepts the userId as a parameter
    private fun enqueueSecurityWork(context: Context, userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(SecurityWorker.KEY_USER_ID, userId)
            .build()

        val securityWorkRequest = OneTimeWorkRequestBuilder<SecurityWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(SecurityWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SecurityWorker.TAG,
            ExistingWorkPolicy.REPLACE,
            securityWorkRequest
        )
    }
}
