package com.app.antitheft.data.api

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.app.antitheft.data.repository.CameraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadCameraWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // In real app use DI; for now, create repo manually
    private val repo = CameraRepository.create()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val userId  = inputData.getString("userId") ?: return@withContext Result.failure()
        val frontPath = inputData.getString("frontPath") ?: return@withContext Result.failure()
        val backPath  = inputData.getString("backPath")  ?: return@withContext Result.failure()

        val frontFile = File(frontPath)
        val backFile  = File(backPath)

        if (!frontFile.exists() || !backFile.exists()) {
            return@withContext Result.failure()
        }

        val success = repo.uploadFrontBack(userId, frontFile, backFile)

        if (success) {
            // Clean up local files if you want
            safeDelete(frontFile)
            safeDelete(backFile)
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun safeDelete(f: File) {
        try {
            if (f.exists()) f.delete()
        } catch (_: Exception) {}
    }

    companion object {

        fun enqueue(context: Context, userId: String, frontPath: String, backPath: String) {
            val data = workDataOf(
                "userId" to userId,
                "frontPath" to frontPath,
                "backPath" to backPath
            )

            val req = OneTimeWorkRequestBuilder<UploadCameraWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(req)
        }
    }
}
