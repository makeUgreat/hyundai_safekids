package com.pony.safe_kids.domain.model

sealed interface CabinSignal {
    data object Loading : CabinSignal

    data class Ready(val state: CabinState) : CabinSignal

    data class Unavailable(val cause: Cause) : CabinSignal {
        sealed interface Cause {
            data object TimedOut : Cause

            data class Failed(val sensor: Sensor) : Cause

            data class Unknown(val sensor: Sensor) : Cause
        }
    }

    companion object {
        fun of(
            doorsOpen: LocationSnapshot<DoorLocation>,
            occupied: LocationSnapshot<SeatLocation>,
            beltBuckled: LocationSnapshot<SeatLocation>,
        ): CabinSignal {
            val snapshots = listOf(
                Sensor.DOOR_POSITION to doorsOpen,
                Sensor.SEAT_OCCUPANCY to occupied,
                Sensor.SEAT_BELT_BUCKLED to beltBuckled,
            )
            snapshots.firstOrNull { (_, snapshot) -> snapshot is LocationSnapshot.Failed }
                ?.let { (sensor, _) -> return Unavailable(Unavailable.Cause.Failed(sensor)) }
            snapshots.firstOrNull { (_, snapshot) -> snapshot is LocationSnapshot.Unknown }
                ?.let { (sensor, _) -> return Unavailable(Unavailable.Cause.Unknown(sensor)) }

            if (doorsOpen !is LocationSnapshot.Ready) return Loading
            if (occupied !is LocationSnapshot.Ready) return Loading
            if (beltBuckled !is LocationSnapshot.Ready) return Loading
            return Ready(
                CabinState(occupied = occupied.values, beltBuckled = beltBuckled.values, doorsOpen = doorsOpen.values)
            )
        }
    }
}
