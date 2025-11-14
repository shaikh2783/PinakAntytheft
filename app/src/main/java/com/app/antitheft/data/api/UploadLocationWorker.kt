package com.app.antitheft.data.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

class UploadLocationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {

        val userId = inputData.getString("userId") ?: return Result.failure()
        val lat = inputData.getDouble("lat", 0.0)
        val lon = inputData.getDouble("lon", 0.0)

        if (lat == 0.0 && lon == 0.0) return Result.retry()

        if (!hasLocationPermission()) return Result.retry()

        val fused = LocationServices.getFusedLocationProviderClient(applicationContext)

        // Try last known
        val lastLoc: Location? = try {
            fused.lastLocation.await()
        } catch (_: Exception) {
            null
        }

        val finalLoc = lastLoc ?: Location("").apply {
            latitude = lat
            longitude = lon
        }

        pushToFirebase(userId, finalLoc)
        val uploaded = uploadToApi(userId, finalLoc)

        return if (uploaded) Result.success() else Result.retry()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun pushToFirebase(userId: String, loc: Location) {
        FirebaseDatabase.getInstance()
            .getReference("users/$userId/Tracking").apply {
                child("location").setValue(
                    mapOf(
                        "latitude" to loc.latitude,
                        "longitude" to loc.longitude
                    )
                )
                child("isLocationRequire").setValue(false)
            }
    }

    private fun uploadToApi(userId: String, loc: Location): Boolean {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", userId)
            .addFormDataPart("type", "location")
            .addFormDataPart("latitude", String.format(Locale.US, "%.6f", loc.latitude))
            .addFormDataPart("longitude", String.format(Locale.US, "%.6f", loc.longitude))
            .build()

        val req = Request.Builder()
            .url("https://login.pinaksecurity.com/api/update/location")
            .post(requestBody)
            .build()

        return try {
            client.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: IOException) {
            false
        }
    }

    companion object {

        fun enqueue(context: Context, userId: String, lat: Double, lon: Double) {
            val data = workDataOf(
                "userId" to userId,
                "lat" to lat,
                "lon" to lon
            )

            val req = OneTimeWorkRequestBuilder<UploadLocationWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(req)
        }
    }
}

