package com.pony.safe_kids.data.vehicle

import com.pony.safe_kids.domain.model.CabinSignal
import kotlinx.coroutines.flow.Flow

interface VehicleSignalSource {
    val cabinSignal: Flow<CabinSignal>
}
