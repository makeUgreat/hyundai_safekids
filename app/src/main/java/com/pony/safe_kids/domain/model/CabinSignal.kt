package com.pony.safe_kids.domain.model

sealed interface CabinSignal {
    data object Loading : CabinSignal

    data class Ready(val state: CabinState) : CabinSignal

    companion object {
        fun of(
            doorsOpen: LocationSnapshot<DoorLocation>,
            occupied: LocationSnapshot<SeatLocation>,
            beltBuckled: LocationSnapshot<SeatLocation>,
        ): CabinSignal {
            if (doorsOpen !is LocationSnapshot.Ready) return Loading
            if (occupied !is LocationSnapshot.Ready) return Loading
            if (beltBuckled !is LocationSnapshot.Ready) return Loading
            return Ready(
                CabinState(occupied = occupied.values, beltBuckled = beltBuckled.values, doorsOpen = doorsOpen.values)
            )
        }
    }
}
