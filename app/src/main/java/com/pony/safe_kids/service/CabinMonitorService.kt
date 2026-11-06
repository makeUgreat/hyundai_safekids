package com.pony.safe_kids.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.pony.safe_kids.R
import com.pony.safe_kids.data.vehicle.PleosVehicleSignalSource
import com.pony.safe_kids.data.vehicle.VehicleSignalSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CabinMonitorService : Service() {

    private val scope = MainScope()
    private lateinit var signalSource: VehicleSignalSource

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CabinMonitorService created")

        createNotificationChannel()
        startInForeground()

        signalSource = PleosVehicleSignalSource(applicationContext)
        signalSource.start()

        scope.launch {
            signalSource.cabinSignal.collect { signal ->
                Log.d(TAG, "CabinSignal $signal")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "CabinMonitorService destroyed")
        scope.cancel()
        signalSource.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.cabin_monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("InlinedApi")
    private fun startInForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        Log.d(TAG, "CabinMonitorService started in foreground")
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.cabin_monitor_notification_title))
            .setOngoing(true)
            .build()

    private companion object {
        const val TAG = "SafeKids"
        const val CHANNEL_ID = "cabin_monitor"
        const val NOTIFICATION_ID = 1
    }
}
