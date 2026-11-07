package com.pony.safe_kids.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Door
import ai.pleos.playground.vehicle.api.Seat
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
import com.pony.safe_kids.domain.model.DoorLocation
import com.pony.safe_kids.domain.model.SeatLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class PleosVehicleSignalSource(context: Context) : VehicleSignalSource {

    private val appContext = context.applicationContext

    override val cabinSignal: Flow<CabinSignal> = flow {
        val vehicle = Vehicle(appContext)
        vehicle.initialize()
        try {
            val door = vehicle.getDoor()
            val seat = vehicle.getSeat()
            emitAll(
                combine(
                    areaSignalFlow(DoorPositionSource(door), ::logError),
                    areaSignalFlow(SeatOccupancySource(seat), ::logError),
                    areaSignalFlow(SeatBeltBuckledSource(seat), ::logError),
                ) { doors, occupied, beltBuckled ->
                    CabinSignal.of(doorsOpen = doors, occupied = occupied, beltBuckled = beltBuckled)
                }.distinctUntilChanged()
            )
        } finally {
            vehicle.release()
        }
    }

    private fun logError(message: String, e: Throwable) {
        Log.e(TAG, message, e)
    }

    private companion object {
        const val TAG = "SafeKids"
    }
}

private class DoorPositionSource(private val door: Door) : AreaSource<DoorArea, DoorLocation> {

    override val name = "DoorPosition"

    override fun checkCapabilities(onComplete: (Collection<DoorArea>) -> Unit, onFailed: (Exception) -> Unit) {
        door.checkDoorPositionCapabilities(onComplete = { _, areas -> onComplete(areas.keys) }, onFailed = onFailed)
    }

    override fun toLocation(area: DoorArea): DoorLocation? = area.toDoorLocation()

    override fun subscribe(area: DoorArea, onReading: (Reading) -> Unit, onError: (Exception) -> Unit): () -> Unit {
        val listener = object : DoorPositionListener {
            override fun onPositionUpdated(pos: Int?) = onReading(readingOf(pos?.isOpen()))
            override fun onFailed(e: Exception) = onError(e)
        }
        door.registerDoorPosition(area, listener)
        return { door.unregisterDoorPosition(area, listener) }
    }

    override fun read(area: DoorArea, onReading: (Reading) -> Unit, onError: (Exception) -> Unit) {
        door.getCurrentDoorPosition(area, onComplete = { onReading(readingOf(it?.isOpen())) }, onFailed = onError)
    }
}

private class SeatOccupancySource(private val seat: Seat) : AreaSource<SeatArea, SeatLocation> {

    override val name = "SeatOccupancy"

    override fun checkCapabilities(onComplete: (Collection<SeatArea>) -> Unit, onFailed: (Exception) -> Unit) {
        seat.checkSeatOccupancyCapabilities(onComplete = { _, areas -> onComplete(areas) }, onFailed = onFailed)
    }

    override fun toLocation(area: SeatArea): SeatLocation? = area.toSeatLocation()

    override fun subscribe(area: SeatArea, onReading: (Reading) -> Unit, onError: (Exception) -> Unit): () -> Unit {
        val listener = object : SeatOccupancyListener {
            override fun onSeatOccupancyUpdated(occupancy: Boolean?) = onReading(readingOf(occupancy))
            override fun onFailed(e: Exception) = onError(e)
        }
        seat.registerSeatOccupancy(area, listener)
        return { seat.unregisterSeatOccupancy(area, listener) }
    }

    override fun read(area: SeatArea, onReading: (Reading) -> Unit, onError: (Exception) -> Unit) {
        seat.getCurrentSeatOccupancy(area, onComplete = { onReading(readingOf(it)) }, onFailed = onError)
    }
}

private class SeatBeltBuckledSource(private val seat: Seat) : AreaSource<SeatArea, SeatLocation> {

    override val name = "SeatBeltBuckled"

    override fun checkCapabilities(onComplete: (Collection<SeatArea>) -> Unit, onFailed: (Exception) -> Unit) {
        seat.checkSeatBeltBuckledCapabilities(onComplete = { _, areas -> onComplete(areas) }, onFailed = onFailed)
    }

    override fun toLocation(area: SeatArea): SeatLocation? = area.toSeatLocation()

    override fun subscribe(area: SeatArea, onReading: (Reading) -> Unit, onError: (Exception) -> Unit): () -> Unit {
        val listener = object : SeatBeltBuckledStateListener {
            override fun onSeatBeltBuckledStateUpdated(state: Boolean?) = onReading(readingOf(state))
            override fun onFailed(e: Exception) = onError(e)
        }
        seat.registerSeatBeltBuckledState(area, listener)
        return { seat.unregisterSeatBeltBuckledState(area, listener) }
    }

    override fun read(area: SeatArea, onReading: (Reading) -> Unit, onError: (Exception) -> Unit) {
        seat.getCurrentSeatBeltBuckledState(area, onComplete = { onReading(readingOf(it)) }, onFailed = onError)
    }
}

private fun Int.isOpen(): Boolean = this > 0

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
