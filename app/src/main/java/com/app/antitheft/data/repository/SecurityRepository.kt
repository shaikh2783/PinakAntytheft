package com.app.antitheft.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.app.antitheft.receiver.NonVisibleLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SecurityRepository(private val context: Context) {

    companion object {
        private const val TAG = "SecurityRepository"
    }

    /**
     * Gets the current location. Assumes permissions are already granted.
     * Returns null on failure.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            Log.d(TAG, "Location acquired: Lat=${location?.latitude}, Lon=${location?.longitude}")
            location
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get location", e)
            null
        }
    }

    /**
     * Captures a photo using CameraX in the background.
     * @param lensFacing The camera to use (front or back).
     * @return The File object of the saved image, or null on failure.
     */
    suspend fun capturePhoto(lensFacing: Int): File? = suspendCoroutine { cont ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val lifecycleOwner = NonVisibleLifecycleOwner()

            try {
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setJpegQuality(80)
                    .setTargetResolution(Size(1280, 720))
                    .build()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )

                val file = createTempFileForPhoto(if (lensFacing == CameraSelector.LENS_FACING_FRONT) "front" else "back")
                val options = ImageCapture.OutputFileOptions.Builder(file).build()

                imageCapture.takePicture(
                    options,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            lifecycleOwner.destroy()
                            cameraProvider.unbindAll()
                            cont.resume(file)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Camera capture failed: ${exc.message}", exc)
                            lifecycleOwner.destroy()
                            cameraProvider.unbindAll()
                            cont.resume(null)
                        }
                    }
                )
            } catch (e: Exception) {
                lifecycleOwner.destroy()
                cameraProvider.unbindAll()
                cont.resume(null)
            }
        }, mainExecutor)
    }

    /**
     * Uploads the captured data to the Pinak Security server.
     */
    suspend fun uploadSecurityData(userId: String, lat: Double?, lon: Double?, frontImage: File?, backImage: File?) {
        withContext(Dispatchers.IO) {
            if (frontImage == null && backImage == null) {
                Log.w(TAG, "Upload skipped: no images were captured.")
                return@withContext
            }

            val client = OkHttpClient()
            val url = "https://login.pinaksecurity.com/api/update/track_data"
            Log.d(TAG, "Uploading data to $url")

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", userId)
                .addFormDataPart("type", "track")
                .addFormDataPart("latitude", lat?.toString() ?: "0.0")
                .addFormDataPart("longitude", lon?.toString() ?: "0.0")

            frontImage?.let {
                multipartBody.addFormDataPart("front_image", it.name, it.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            }
            backImage?.let {
                multipartBody.addFormDataPart("back_image", it.name, it.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            }

            val request = Request.Builder().url(url).post(multipartBody.build()).build()

            try {
                client.newCall(request).execute().use { response ->
                    handleUploadResponse(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload network error", e)
            } finally {
                // Clean up the temporary image files after upload attempt
                frontImage?.delete()
                backImage?.delete()
                Log.d(TAG, "Temporary image files deleted.")
            }
        }
    }

    private fun handleUploadResponse(response: Response) {
        val responseBody = response.body?.string()
        if (response.isSuccessful) {
            Log.d(TAG, "Upload success: $responseBody")
        } else {
            Log.e(TAG, "Upload failed with code ${response.code}: $responseBody")
        }
    }

    private fun createTempFileForPhoto(tag: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File.createTempFile("IMG_${timeStamp}_${tag}_", ".jpg", context.cacheDir)
    }
}
