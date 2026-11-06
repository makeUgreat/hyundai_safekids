package com.pony.safe_kids

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pony.safe_kids.data.vehicle.PleosVehicleSignalSource
import com.pony.safe_kids.data.vehicle.VehicleSignalSource
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var signalSource: VehicleSignalSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity created")

        signalSource = PleosVehicleSignalSource(applicationContext)
        signalSource.start()

        lifecycleScope.launch {
            signalSource.cabinSignal.collect { signal ->
                Log.d(TAG, "CabinSignal $signal")
            }
        }
    }

    override fun onDestroy() {
        signalSource.stop()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SafeKids"
    }
}
