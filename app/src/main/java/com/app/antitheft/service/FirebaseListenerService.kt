package com.app.antitheft.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import com.google.firebase.database.*
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.app.antitheft.R
import androidx.core.content.edit
import androidx.lifecycle.ProcessLifecycleOwner
import com.app.antitheft.MainActivity

class FirebaseListenerService : Service() {

    private lateinit var db: FirebaseDatabase
    private var refLocation: DatabaseReference? = null
    private var refCamera: DatabaseReference? = null

    private var userId: String? = null
    private var locListener: ValueEventListener? = null
    private var camListener: ValueEventListener? = null


    override fun onCreate() {
        super.onCreate()

        // 1. Create the Notification Channel first.
        createChannel()

        db = FirebaseDatabase.getInstance()
        // 2. Build the notification. Now the channel "loc_ch" exists.
        val notif = NotificationCompat.Builder(this, "loc_ch")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pinak Security")
            .setContentText("Tracking location…")
            .setOngoing(true)
            .build()

        // 3. Now it is safe to call startForeground().
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                100,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(100, notif)
        }
    }

    // The createChannel() method remains the same.
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "loc_ch",
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val fromIntent = intent?.getStringExtra("userId")
        if (fromIntent != null) {
            userId = fromIntent
            getSharedPreferences("prefs", MODE_PRIVATE)
                .edit { putString("userId", userId) }
        } else {
            userId = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("userId", null)
        }

        if (userId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startFirebaseListeners()

        return START_STICKY
    }


    private fun startFirebaseListeners() {
        val uid = userId ?: return

        if (refLocation == null) {
            refLocation = db.getReference("users/$uid/Tracking/isLocationRequire")
        }

        if (locListener == null) {
            locListener = object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val need = snapshot.getValue(Boolean::class.java) ?: false

                    if (need) {
                        startLocationService()
                    } else {
                        stopService(Intent(this@FirebaseListenerService, LocationService::class.java))
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            refLocation!!.addValueEventListener(locListener!!)
        }
        // -------- CAMERA FLAG (NEW) ----------
        if (refCamera == null) {
            refCamera = db.getReference("users/$uid/CaptureImage/isCaptureImage")
        }

        if (camListener == null) {
            camListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val need = snapshot.getValue(Boolean::class.java) ?: false

                    if (need) {
                        // Try to start camera directly, else show prompt
                        if (canStartCameraFgs()) {
                            startCameraService()
                        } else {
                            showCapturePromptNotification()
                        }
                    } else {
                        stopService(Intent(this@FirebaseListenerService, CameraService::class.java))
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            refCamera!!.addValueEventListener(camListener!!)
        }
    }

    private fun startLocationService() {
        val i = Intent(this, LocationService::class.java)
        i.putExtra("userId", userId)

        // LocationService can be a foreground service → allowed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(i)
        } else {
            startService(i)
        }
    }
    // ------- NEW: check if we are allowed to start camera FGS -------
    private fun canStartCameraFgs(): Boolean {
        val hasCamPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCamPermission) return false

        // Android 14+: camera FGS only when app is visible
        return if (Build.VERSION.SDK_INT >= 34) {
            ProcessLifecycleOwner.get().lifecycle.currentState
                .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        } else {
            true
        }
    }

    // ------- NEW: show notification to open MainActivity and ask for camera -------
    private fun showCapturePromptNotification() {
        val channelId = "capture_prompt_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Camera Request",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_REQUEST_CAMERA_CAPTURE"
            putExtra("userId", userId)
        }

        val pi = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Allow camera capture")
            .setContentText("Tap to capture front & back images")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(2001, notif)
    }

    // ------- NEW: actually start CameraService -------
    private fun startCameraService() {
        val i = Intent(this, CameraService::class.java).apply {
            putExtra("userId", userId)
            putExtra("mode", "front_back") // e.g. front + back
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, i)
        } else {
            startService(i)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        locListener?.let { refLocation?.removeEventListener(it) }
        camListener?.let { refCamera?.removeEventListener(it) }


    }

    override fun onBind(intent: Intent?): IBinder? = null
}
