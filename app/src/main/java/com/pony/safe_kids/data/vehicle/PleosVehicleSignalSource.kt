package com.pony.safe_kids.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import android.content.Context
import android.util.Log
import com.pony.safe_kids.domain.model.CabinSignal
import com.pony.safe_kids.domain.model.CabinState
import com.pony.safe_kids.domain.model.DoorLocation
import com.pony.safe_kids.domain.model.SeatLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PleosVehicleSignalSource(context: Context) : VehicleSignalSource {

    private val vehicle = Vehicle(context.applicationContext)

    private val lock = Any()
    private var snapshot = Snapshot()

    private val _cabinSignal = MutableStateFlow<CabinSignal>(CabinSignal.Loading)
    override val cabinSignal: StateFlow<CabinSignal> = _cabinSignal.asStateFlow()

    override fun start() {
        vehicle.initialize()
        subscribeDoors()
        subscribeSeatOccupancy()
        subscribeSeatBelts()
    }

    override fun stop() {
        vehicle.release()
    }

    private fun subscribeDoors() {
        val door = vehicle.getDoor()
        door.checkDoorPositionCapabilities(
            onComplete = { _, areas ->
                val supported = areas.keys.mapNotNull { area -> area.toDoorLocation()?.let { area to it } }
                updateSnapshot { it.copy(doors = supported.map { (_, location) -> location }.toSet()) }
                supported.forEach { (area, location) ->
                    val listener = object : DoorPositionListener {
                        override fun onPositionUpdated(pos: Int?) {
                            val open = pos?.let { it > 0 }
                            updateSnapshot { snapshot ->
                                snapshot.copy(doorsOpen = snapshot.doorsOpen + (location to open))
                            }
                        }

                        override fun onFailed(e: Exception) {
                            Log.e(TAG, "DoorPosition $location failed", e)
                        }
                    }
                    door.registerDoorPosition(area, listener)
                    door.getCurrentDoorPosition(
                        area = area,
                        onComplete = { pos ->
                            val open = pos?.let { it > 0 }
                            updateSnapshot { snapshot ->
                                snapshot.copy(doorsOpen = snapshot.doorsOpen.plusIfAbsent(location, open))
                            }
                        },
                        onFailed = { e -> Log.e(TAG, "DoorPosition $location get failed", e) },
                    )
                }
            },
            onFailed = { e -> Log.e(TAG, "DoorPosition capability failed", e) },
        )
    }

    private fun subscribeSeatOccupancy() {
        val seat = vehicle.getSeat()
        seat.checkSeatOccupancyCapabilities(
            onComplete = { _, areas ->
                val supported = areas.mapNotNull { area -> area.toSeatLocation()?.let { area to it } }
                updateSnapshot { it.copy(occupancySeats = supported.map { (_, location) -> location }.toSet()) }
                supported.forEach { (area, location) ->
                    val listener = object : SeatOccupancyListener {
                        override fun onSeatOccupancyUpdated(occupancy: Boolean?) {
                            updateSnapshot { snapshot ->
                                snapshot.copy(occupied = snapshot.occupied + (location to occupancy))
                            }
                        }

                        override fun onFailed(e: Exception) {
                            Log.e(TAG, "SeatOccupancy $location failed", e)
                        }
                    }
                    seat.registerSeatOccupancy(area, listener)
                    seat.getCurrentSeatOccupancy(
                        area = area,
                        onComplete = { occupancy ->
                            updateSnapshot { snapshot ->
                                snapshot.copy(occupied = snapshot.occupied.plusIfAbsent(location, occupancy))
                            }
                        },
                        onFailed = { e -> Log.e(TAG, "SeatOccupancy $location get failed", e) },
                    )
                }
            },
            onFailed = { e -> Log.e(TAG, "SeatOccupancy capability failed", e) },
        )
    }

    private fun subscribeSeatBelts() {
        val seat = vehicle.getSeat()
        seat.checkSeatBeltBuckledCapabilities(
            onComplete = { _, areas ->
                val supported = areas.mapNotNull { area -> area.toSeatLocation()?.let { area to it } }
                updateSnapshot { it.copy(beltSeats = supported.map { (_, location) -> location }.toSet()) }
                supported.forEach { (area, location) ->
                    val listener = object : SeatBeltBuckledStateListener {
                        override fun onSeatBeltBuckledStateUpdated(state: Boolean?) {
                            updateSnapshot { snapshot ->
                                snapshot.copy(beltBuckled = snapshot.beltBuckled + (location to state))
                            }
                        }

                        override fun onFailed(e: Exception) {
                            Log.e(TAG, "SeatBeltBuckled $location failed", e)
                        }
                    }
                    seat.registerSeatBeltBuckledState(area, listener)
                    seat.getCurrentSeatBeltBuckledState(
                        area = area,
                        onComplete = { state ->
                            updateSnapshot { snapshot ->
                                snapshot.copy(beltBuckled = snapshot.beltBuckled.plusIfAbsent(location, state))
                            }
                        },
                        onFailed = { e -> Log.e(TAG, "SeatBeltBuckled $location get failed", e) },
                    )
                }
            },
            onFailed = { e -> Log.e(TAG, "SeatBeltBuckled capability failed", e) },
        )
    }

    private fun updateSnapshot(change: (Snapshot) -> Snapshot) {
        synchronized(lock) {
            snapshot = change(snapshot)
            _cabinSignal.value = snapshot.toCabinSignal()
        }
    }

    private data class Snapshot(
        val doors: Set<DoorLocation>? = null,
        val occupancySeats: Set<SeatLocation>? = null,
        val beltSeats: Set<SeatLocation>? = null,
        val doorsOpen: Map<DoorLocation, Boolean?> = emptyMap(),
        val occupied: Map<SeatLocation, Boolean?> = emptyMap(),
        val beltBuckled: Map<SeatLocation, Boolean?> = emptyMap(),
    ) {
        fun toCabinSignal(): CabinSignal {
            val doors = doors ?: return CabinSignal.Loading
            val occupancySeats = occupancySeats ?: return CabinSignal.Loading
            val beltSeats = beltSeats ?: return CabinSignal.Loading

            return CabinSignal.Ready(
                CabinState(
                    occupied = occupancySeats.associateWith { occupied[it] ?: return CabinSignal.Loading },
                    beltBuckled = beltSeats.associateWith { beltBuckled[it] ?: return CabinSignal.Loading },
                    doorsOpen = doors.associateWith { doorsOpen[it] ?: return CabinSignal.Loading },
                )
            )
        }
    }

    private fun <K, V> Map<K, V>.plusIfAbsent(key: K, value: V): Map<K, V> =
        if (key in this) this else this + (key to value)

    private fun SeatArea.toSeatLocation(): SeatLocation? = when (values.singleOrNull()) {
        SeatValue.ROW_1_LEFT -> SeatLocation.FRONT_LEFT
        SeatValue.ROW_1_RIGHT -> SeatLocation.FRONT_RIGHT
        SeatValue.ROW_2_LEFT -> SeatLocation.REAR_LEFT
        SeatValue.ROW_2_CENTER -> SeatLocation.REAR_CENTER
        SeatValue.ROW_2_RIGHT -> SeatLocation.REAR_RIGHT
        else -> null
    }

    private fun DoorArea.toDoorLocation(): DoorLocation? = when (values.singleOrNull()) {
        DoorValue.ROW_1_LEFT -> DoorLocation.FRONT_LEFT
        DoorValue.ROW_1_RIGHT -> DoorLocation.FRONT_RIGHT
        DoorValue.ROW_2_LEFT -> DoorLocation.REAR_LEFT
        DoorValue.ROW_2_RIGHT -> DoorLocation.REAR_RIGHT
        else -> null
    }

    private companion object {
        const val TAG = "SafeKids"
    }
}
