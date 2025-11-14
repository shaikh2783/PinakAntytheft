package com.app.antitheft.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.database.*
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.app.antitheft.R

class FirebaseListenerService : Service() {

    private lateinit var db: FirebaseDatabase
    private var refLocation: DatabaseReference? = null

    private var userId: String? = null
    private var locListener: ValueEventListener? = null

    override fun onCreate() {
        super.onCreate()
        db = FirebaseDatabase.getInstance()
        val notif = NotificationCompat.Builder(this, "loc_ch")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pinak Security")
            .setContentText("Tracking location…")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                100,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(100, notif)
        }

        createChannel()
    }
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
                .edit().putString("userId", userId).apply()
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

    override fun onDestroy() {
        super.onDestroy()
        locListener?.let { refLocation?.removeEventListener(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
