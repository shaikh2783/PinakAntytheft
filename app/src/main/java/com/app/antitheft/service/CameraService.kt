package com.app.antitheft.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.app.antitheft.R
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutionException
import android.util.Size
import android.content.pm.ServiceInfo
import com.app.antitheft.data.api.UploadCameraWorker
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
class CameraService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "camera_channel"
        private const val PREFS_NAME = "LocationPrefs"
        private const val NOTIFICATION_ID = 2
    }

    private var imageCapture: ImageCapture? = null
    private lateinit var userRef: com.google.firebase.database.DatabaseReference
    private var userId: String = "100"

    private var frontFile: File? = null
    private var backFile: File? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pinak Security")
            .setContentText("Camera service running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: RuntimeException) {
            Log.w("CameraService", "FGS promotion not allowed in onCreate", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null && intent.hasExtra("userId")) {
            userId = intent.getStringExtra("userId") ?: userId
        }

        userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("CaptureImage")

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit {
                putString("userId", userId)
            }

        // You can either let FirebaseListenerService trigger this,
        // or (like your old code) listen directly here:
        listenForCaptureRequests()

        return START_STICKY
    }

    private fun listenForCaptureRequests() {
        userRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val captureImage =
                    snapshot.child("isCaptureImage").getValue(Boolean::class.java)

                if (captureImage == true) {
                    Log.d("CameraService", "Capture request received")
                    captureBothPhotos()
                    userRef.child("isCaptureImage").setValue(false)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("CameraService", "Firebase error: ${error.message}")
            }
        })
    }

    private var isCapturing = false

    private fun captureBothPhotos() {
        if (isCapturing) {
            Log.w("CameraService", "Already capturing, ignoring new request")
            return
        }
        isCapturing = true

        takePhoto(CameraSelector.LENS_FACING_BACK, "back") {
            takePhoto(CameraSelector.LENS_FACING_FRONT, "front") {
                val front = frontFile
                val back = backFile
                if (front != null && back != null) {
                    UploadCameraWorker.enqueue(
                        applicationContext,
                        userId,
                        front.absolutePath,
                        back.absolutePath
                    )
                }
                isCapturing = false
            }
        }
    }

    private fun takePhoto(
        lensFacing: Int,
        tag: String,
        onComplete: (() -> Unit)?
    ) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        val mainExecutor: Executor = ContextCompat.getMainExecutor(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val imageCaptureLocal = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setJpegQuality(80)
                    .setTargetResolution(Size(1280, 720))
                    .build()

                imageCapture = imageCaptureLocal

                // 🔑 Custom LifecycleOwner in RESUMED state (same as your old Java code)
                val lifecycleOwner = object : LifecycleOwner {
                    private val registry = LifecycleRegistry(this).apply {
                        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                        handleLifecycleEvent(Lifecycle.Event.ON_START)
                        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                    }
                    override val lifecycle: Lifecycle
                        get() =registry
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCaptureLocal
                )

                val photoFile = File(
                    getExternalFilesDir(null),
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(Date()) + "_${tag}.jpg"
                )

                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(photoFile)
                    .build()

                imageCaptureLocal.takePicture(
                    outputOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(
                            outputFileResults: ImageCapture.OutputFileResults
                        ) {
                            Log.d("CameraService", "Saved: ${photoFile.absolutePath}")
                            if (tag == "front") {
                                frontFile = photoFile
                            } else {
                                backFile = photoFile
                            }
                            onComplete?.invoke()
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraService", "Capture failed: ${exc.message}")
                            onComplete?.invoke()
                        }
                    }
                )
            } catch (e: ExecutionException) {
                e.printStackTrace()
                onComplete?.invoke()
            } catch (e: InterruptedException) {
                e.printStackTrace()
                onComplete?.invoke()
            }
        }, mainExecutor)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}
