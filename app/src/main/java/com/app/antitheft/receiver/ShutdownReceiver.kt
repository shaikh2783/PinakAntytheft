package com.app.antitheft.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.app.antitheft.Helper.datastore.DataStoreManager
import com.app.antitheft.worker.SecurityWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking // <-- IMPORTANT: Import runBlocking

class ShutdownReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SHUTDOWN) {
            Log.w("ShutdownReceiver", "ACTION_SHUTDOWN detected! Starting synchronous work to enqueue task.")

            // For the brief and critical shutdown event, we must ensure our code runs.
            // runBlocking will pause the onReceive thread until the DataStore read is complete.
            // This is acceptable because the operation is very fast.
            runBlocking {
                try {
                    val dataStore = DataStoreManager.getInstance(context)
                    val userId = dataStore.getUserId.first()

                    if (!userId.isNullOrEmpty()) {
                        Log.d("ShutdownReceiver", "Found User ID: $userId. Enqueuing work.")
                        enqueueSecurityWork(context, userId)
                    } else {
                        Log.e("ShutdownReceiver", "Could not find a valid User ID in DataStore. Aborting.")
                    }
                } catch (e: Exception) {
                    Log.e("ShutdownReceiver", "Error during shutdown work: ${e.message}", e)
                }
            }
            Log.d("ShutdownReceiver", "onReceive method finished.")
        }
    }

    private fun enqueueSecurityWork(context: Context, userId: String) {
        // This logic remains the same.
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

        WorkManager.getInstance(context).enqueue(securityWorkRequest)
        Log.d("ShutdownReceiver", "Security work has been successfully enqueued with WorkManager.")
    }
}
