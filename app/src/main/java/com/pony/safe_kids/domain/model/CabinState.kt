package com.pony.safe_kids.domain.model

data class SeatSignals(
    val occupied: Signal<Boolean>,
    val beltBuckled: Signal<Boolean>,
)

data class CabinState(
    private val occupied: Map<SeatLocation, Boolean>,
    private val beltBuckled: Map<SeatLocation, Boolean>,
    private val doorsOpen: Map<DoorLocation, Boolean>,
) {
    fun seat(location: SeatLocation): SeatSignals = SeatSignals(
        occupied = occupied.signalOf(location),
        beltBuckled = beltBuckled.signalOf(location),
    )

    fun door(location: DoorLocation): Signal<Boolean> = doorsOpen.signalOf(location)

    private fun <K, V : Any> Map<K, V>.signalOf(key: K): Signal<V> =
        get(key)?.let { Signal.Supported(it) } ?: Signal.Unsupported
}
