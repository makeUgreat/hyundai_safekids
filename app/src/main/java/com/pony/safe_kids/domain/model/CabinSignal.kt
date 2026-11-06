package com.pony.safe_kids.domain.model

sealed interface CabinSignal {
    data object Loading : CabinSignal

    data class Ready(val state: CabinState) : CabinSignal
}
