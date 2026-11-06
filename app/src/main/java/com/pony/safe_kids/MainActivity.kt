package com.pony.safe_kids

import ai.pleos.playground.vehicle.Vehicle
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var vehicle: Vehicle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity created")

        vehicle = Vehicle(applicationContext)
        vehicle.initialize()
        checkSignalCapabilities()
    }

    override fun onDestroy() {
        vehicle.release()
        super.onDestroy()
    }

    private fun checkSignalCapabilities() {
        vehicle.getDoor().checkDoorPositionCapabilities(
            onComplete = { _, _ -> Log.d(TAG, "DoorPosition capability ok") },
            onFailed = { e -> Log.e(TAG, "DoorPosition capability failed", e) },
        )

        vehicle.getSeat().checkSeatOccupancyCapabilities(
            onComplete = { _, _ -> Log.d(TAG, "SeatOccupancy capability ok") },
            onFailed = { e -> Log.e(TAG, "SeatOccupancy capability failed", e) },
        )

        vehicle.getSeat().checkSeatBeltBuckledCapabilities(
            onComplete = { _, _ -> Log.d(TAG, "SeatBeltBuckled capability ok") },
            onFailed = { e -> Log.e(TAG, "SeatBeltBuckled capability failed", e) },
        )
    }

    private companion object {
        const val TAG = "SafeKids"
    }
}
