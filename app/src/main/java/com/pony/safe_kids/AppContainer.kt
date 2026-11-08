package com.pony.safe_kids

import android.content.Context
import com.pony.safe_kids.data.vehicle.PleosVehicleSignalSource
import com.pony.safe_kids.data.vehicle.VehicleSignalSource
import com.pony.safe_kids.domain.model.CabinSignal
import com.pony.safe_kids.domain.model.unavailableAfter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AppContainer(context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val signalSource: VehicleSignalSource = PleosVehicleSignalSource(context)

    val cabinSignal: StateFlow<CabinSignal> = signalSource.cabinSignal
        .unavailableAfter(SIGNAL_TIMEOUT)
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(stopTimeout = 5.seconds, replayExpiration = Duration.ZERO),
            initialValue = CabinSignal.Loading,
        )

    private companion object {
        val SIGNAL_TIMEOUT = 10.seconds
    }
}
