package com.app.antitheft.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.app.antitheft.data.api.UploadLocationWorker
import com.app.antitheft.data.datasource.FirebaseLocationFlagDataSource

class LocationRepository(private val context: Context) {

    fun startListening(userId: String) {
        FirebaseLocationFlagDataSource.observeLocationFlag(userId) { required ->
            if (required) {
                triggerOneTimeLocationUpload()
            }
        }
    }

    private fun triggerOneTimeLocationUpload() {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val uid = prefs.getString("userId", null) ?: return

        val data = workDataOf("uid" to uid)

        val request = OneTimeWorkRequestBuilder<UploadLocationWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

}
