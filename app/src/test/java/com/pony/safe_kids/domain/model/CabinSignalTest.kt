package com.pony.safe_kids.domain.model

import com.pony.safe_kids.domain.model.CabinSignal.Unavailable.Cause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinSignalTest {

    private val doors = LocationSnapshot.Ready(mapOf(DoorLocation.REAR_LEFT to true))
    private val occupied = LocationSnapshot.Ready(mapOf(SeatLocation.REAR_LEFT to true))
    private val beltBuckled = LocationSnapshot.Ready(mapOf(SeatLocation.REAR_LEFT to false))

    private val unknownDoor = LocationSnapshot.Unknown(setOf(DoorLocation.REAR_LEFT))
    private val unknownSeat = LocationSnapshot.Unknown(setOf(SeatLocation.REAR_LEFT))

    @Test
    fun `is loading when any snapshot is pending`() {
        assertEquals(CabinSignal.Loading, CabinSignal.of(LocationSnapshot.Pending, occupied, beltBuckled))
        assertEquals(CabinSignal.Loading, CabinSignal.of(doors, LocationSnapshot.Pending, beltBuckled))
        assertEquals(CabinSignal.Loading, CabinSignal.of(doors, occupied, LocationSnapshot.Pending))
    }

    @Test
    fun `is unavailable with the sensor whose value is unknown`() {
        assertEquals(unavailable(Cause.Unknown(Sensor.DOOR_POSITION)), CabinSignal.of(unknownDoor, occupied, beltBuckled))
        assertEquals(unavailable(Cause.Unknown(Sensor.SEAT_OCCUPANCY)), CabinSignal.of(doors, unknownSeat, beltBuckled))
        assertEquals(unavailable(Cause.Unknown(Sensor.SEAT_BELT_BUCKLED)), CabinSignal.of(doors, occupied, unknownSeat))
    }

    @Test
    fun `is unavailable with the sensor whose capability check failed`() {
        val failed = LocationSnapshot.Failed

        assertEquals(unavailable(Cause.Failed(Sensor.DOOR_POSITION)), CabinSignal.of(failed, occupied, beltBuckled))
        assertEquals(unavailable(Cause.Failed(Sensor.SEAT_OCCUPANCY)), CabinSignal.of(doors, failed, beltBuckled))
        assertEquals(unavailable(Cause.Failed(Sensor.SEAT_BELT_BUCKLED)), CabinSignal.of(doors, occupied, failed))
    }

    @Test
    fun `failure wins over unknown and unknown wins over pending`() {
        assertEquals(
            unavailable(Cause.Failed(Sensor.SEAT_BELT_BUCKLED)),
            CabinSignal.of(unknownDoor, occupied, LocationSnapshot.Failed),
        )
        assertEquals(
            unavailable(Cause.Unknown(Sensor.SEAT_OCCUPANCY)),
            CabinSignal.of(LocationSnapshot.Pending, unknownSeat, beltBuckled),
        )
    }

    @Test
    fun `is ready with every signal when all snapshots are ready`() {
        val signal = CabinSignal.of(doorsOpen = doors, occupied = occupied, beltBuckled = beltBuckled)

        assertTrue(signal is CabinSignal.Ready)
        val state = (signal as CabinSignal.Ready).state
        assertEquals(Signal.Supported(true), state.door(DoorLocation.REAR_LEFT))
        assertEquals(Signal.Supported(true), state.seat(SeatLocation.REAR_LEFT).occupied)
        assertEquals(Signal.Supported(false), state.seat(SeatLocation.REAR_LEFT).beltBuckled)
        assertEquals(Signal.Unsupported, state.door(DoorLocation.FRONT_LEFT))
    }

    private fun unavailable(cause: Cause) = CabinSignal.Unavailable(cause)
}
