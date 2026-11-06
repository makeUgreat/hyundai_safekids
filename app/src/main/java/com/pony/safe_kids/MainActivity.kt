package com.pony.safe_kids

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
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
            onComplete = { _, areas ->
                Log.d(TAG, "DoorPosition capability ok")
                areas.keys
                    .filterNot { DoorValue.HOOD in it.values || DoorValue.REAR in it.values }
                    .forEach(::subscribeDoorPosition)
            },
            onFailed = { e -> Log.e(TAG, "DoorPosition capability failed", e) },
        )

        vehicle.getSeat().checkSeatOccupancyCapabilities(
            onComplete = { _, areas ->
                Log.d(TAG, "SeatOccupancy capability ok")
                areas.forEach(::subscribeSeatOccupancy)
            },
            onFailed = { e -> Log.e(TAG, "SeatOccupancy capability failed", e) },
        )

        vehicle.getSeat().checkSeatBeltBuckledCapabilities(
            onComplete = { _, areas ->
                Log.d(TAG, "SeatBeltBuckled capability ok")
                areas.forEach(::subscribeSeatBeltBuckled)
            },
            onFailed = { e -> Log.e(TAG, "SeatBeltBuckled capability failed", e) },
        )
    }

    private fun subscribeDoorPosition(area: DoorArea) {
        val name = area.values.joinToString("+")
        vehicle.getDoor().registerDoorPosition(area, object : DoorPositionListener {
            override fun onPositionUpdated(pos: Int?) {
                Log.d(TAG, "DoorPosition $name=$pos")
            }

            override fun onFailed(e: Exception) {
                Log.e(TAG, "DoorPosition $name failed", e)
            }
        })
    }

    private fun subscribeSeatOccupancy(area: SeatArea) {
        val name = area.values.joinToString("+")
        vehicle.getSeat().registerSeatOccupancy(area, object : SeatOccupancyListener {
            override fun onSeatOccupancyUpdated(occupancy: Boolean?) {
                Log.d(TAG, "SeatOccupancy $name=$occupancy")
            }

            override fun onFailed(e: Exception) {
                Log.e(TAG, "SeatOccupancy $name failed", e)
            }
        })
    }

    private fun subscribeSeatBeltBuckled(area: SeatArea) {
        val name = area.values.joinToString("+")
        vehicle.getSeat().registerSeatBeltBuckledState(area, object : SeatBeltBuckledStateListener {
            override fun onSeatBeltBuckledStateUpdated(state: Boolean?) {
                Log.d(TAG, "SeatBeltBuckled $name=$state")
            }

            override fun onFailed(e: Exception) {
                Log.e(TAG, "SeatBeltBuckled $name failed", e)
            }
        })
    }

    private companion object {
        const val TAG = "SafeKids"
    }
}
