package com.pony.safe_kids

import android.content.Context
import com.pony.safe_kids.data.vehicle.PleosVehicleSignalSource
import com.pony.safe_kids.data.vehicle.VehicleSignalSource
import com.pony.safe_kids.domain.model.CabinSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppContainer(context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val signalSource: VehicleSignalSource = PleosVehicleSignalSource(context)

    val cabinSignal: StateFlow<CabinSignal> = signalSource.cabinSignal.stateIn(
        scope = applicationScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0),
        initialValue = CabinSignal.Loading,
    )
}
