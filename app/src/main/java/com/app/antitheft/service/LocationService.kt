package com.app.antitheft.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.app.antitheft.R
import com.app.antitheft.data.api.UploadLocationWorker
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class LocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private lateinit var request: LocationRequest

    private var userId: String? = null

    override fun onCreate() {
        super.onCreate()

        createChannel()

        val notif = NotificationCompat.Builder(this, "loc_ch")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pinak Security")
            .setContentText("Tracking location…")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(200, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(200, notif)
        }

        fused = LocationServices.getFusedLocationProviderClient(this)

        request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                15000
            )
                .setMinUpdateIntervalMillis(5000)
                .setWaitForAccurateLocation(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            LocationRequest.create().apply {
                interval = 15000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                pushLocation(loc)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getStringExtra("userId")
        startUpdates()
        return START_STICKY
    }

    private fun startUpdates() {

        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bg = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if (bg != PackageManager.PERMISSION_GRANTED) {
                stopSelf()
                return
            }
        }

        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun pushLocation(loc: Location) {
        val uid = userId ?: return

        // Update Firebase
        FirebaseDatabase.getInstance()
            .getReference("users/$uid/Tracking")
            .apply {
                child("location").setValue(
                    mapOf(
                        "latitude" to loc.latitude,
                        "longitude" to loc.longitude
                    )
                )
                child("isLocationRequire").setValue(false)
            }

        // Upload to backend
        UploadLocationWorker.enqueue(
            this,
            uid,
            loc.latitude,
            loc.longitude
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        fused.removeLocationUpdates(callback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
}
