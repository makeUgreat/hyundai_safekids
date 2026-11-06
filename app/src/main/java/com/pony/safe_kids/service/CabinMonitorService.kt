package com.pony.safe_kids.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
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

        signalSource = PleosVehicleSignalSource(applicationContext)
        signalSource.start()

        scope.launch {
            signalSource.cabinSignal.collect { signal ->
                Log.d(TAG, "CabinSignal $signal")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "CabinMonitorService destroyed")
        scope.cancel()
        signalSource.stop()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SafeKids"
    }
}
