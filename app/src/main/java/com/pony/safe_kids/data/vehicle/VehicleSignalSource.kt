package com.pony.safe_kids.data.vehicle

import com.pony.safe_kids.domain.model.CabinSignal
import kotlinx.coroutines.flow.StateFlow

interface VehicleSignalSource {
    val cabinSignal: StateFlow<CabinSignal>

    fun start()

    fun stop()
}
