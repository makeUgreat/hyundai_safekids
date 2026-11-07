package com.pony.safe_kids.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinSignalTest {

    private val doors = LocationSnapshot.Ready(mapOf(DoorLocation.REAR_LEFT to true))
    private val occupied = LocationSnapshot.Ready(mapOf(SeatLocation.REAR_LEFT to true))
    private val beltBuckled = LocationSnapshot.Ready(mapOf(SeatLocation.REAR_LEFT to false))

    @Test
    fun `is loading when any snapshot is pending`() {
        assertEquals(CabinSignal.Loading, CabinSignal.of(LocationSnapshot.Pending, occupied, beltBuckled))
        assertEquals(CabinSignal.Loading, CabinSignal.of(doors, LocationSnapshot.Pending, beltBuckled))
        assertEquals(CabinSignal.Loading, CabinSignal.of(doors, occupied, LocationSnapshot.Pending))
    }

    @Test
    fun `is loading when any snapshot is unknown`() {
        val unknownDoor = LocationSnapshot.Unknown(setOf(DoorLocation.REAR_LEFT))
        val unknownSeat = LocationSnapshot.Unknown(setOf(SeatLocation.REAR_LEFT))

        assertEquals(CabinSignal.Loading, CabinSignal.of(unknownDoor, occupied, beltBuckled))
        assertEquals(CabinSignal.Loading, CabinSignal.of(doors, unknownSeat, beltBuckled))
        assertEquals(CabinSignal.Loading, CabinSignal.of(doors, occupied, unknownSeat))
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
}
