package com.pony.safe_kids.domain.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

fun Flow<CabinSignal>.unavailableAfter(timeout: Duration): Flow<CabinSignal> = channelFlow {
    val timer = launch {
        delay(timeout)
        send(CabinSignal.Unavailable(CabinSignal.Unavailable.Cause.TimedOut))
    }
    collect { signal ->
        if (signal !is CabinSignal.Loading) timer.cancel()
        send(signal)
    }
}
