package com.pony.safe_kids

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
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
        logSignalCapabilities()
    }

    override fun onDestroy() {
        vehicle.release()
        super.onDestroy()
    }

    private fun logSignalCapabilities() {
        vehicle.getDoor().checkDoorPositionCapabilities(
            onComplete = { config, areas ->
                val bounds = areas.map { (area, range) -> "${area.label()}=${range.minValue}..${range.maxValue}" }
                Log.d(TAG, "DoorPosition access=${config.accessType} areas=$bounds")
            },
            onFailed = { e -> Log.e(TAG, "DoorPosition capability failed", e) },
        )

        vehicle.getSeat().checkSeatOccupancyCapabilities(
            onComplete = { config, areas ->
                Log.d(TAG, "SeatOccupancy access=${config.accessType} areas=${areas.map { it.label() }}")
            },
            onFailed = { e -> Log.e(TAG, "SeatOccupancy capability failed", e) },
        )

        vehicle.getSeat().checkSeatBeltBuckledCapabilities(
            onComplete = { config, areas ->
                Log.d(TAG, "SeatBeltBuckled access=${config.accessType} areas=${areas.map { it.label() }}")
            },
            onFailed = { e -> Log.e(TAG, "SeatBeltBuckled capability failed", e) },
        )
    }

    private fun DoorArea.label() = values.joinToString("+")

    private fun SeatArea.label() = values.joinToString("+")

    private companion object {
        const val TAG = "SafeKids"
    }
}
