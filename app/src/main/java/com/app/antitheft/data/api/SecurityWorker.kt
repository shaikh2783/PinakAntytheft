package com.app.antitheft.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.antitheft.repository.SecurityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class SecurityWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SecurityWorker"
        const val KEY_USER_ID = "user_id"
    }

    // Instantiate the repository. (For larger apps, consider Dependency Injection)
    private val securityRepository = SecurityRepository(applicationContext)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker starting task.")

        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Worker failed: Missing Camera or Location permissions.")
            return Result.failure()
        }

        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()

        // Perform all tasks concurrently for efficiency
        return withContext(Dispatchers.IO) {
            try {
                val locationDeferred = async { securityRepository.getCurrentLocation() }

                val backImage = securityRepository.capturePhoto(CameraSelector.LENS_FACING_BACK)
                val frontImage = securityRepository.capturePhoto(CameraSelector.LENS_FACING_FRONT)

                val location = locationDeferred.await()

                securityRepository.uploadSecurityData(
                    userId,
                    location?.latitude,
                    location?.longitude,
                    frontImage,
                    backImage
                )

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Worker error", e)
                Result.failure()
            }
        }

    }

    private fun hasRequiredPermissions(): Boolean {
        val hasCameraPerm = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLocationPerm = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return hasCameraPerm && hasLocationPerm
    }
}
