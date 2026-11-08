package com.pony.safe_kids.domain.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CabinSignalTimeoutTest {

    private val ready = CabinSignal.Ready(CabinState(occupied = emptyMap(), beltBuckled = emptyMap(), doorsOpen = emptyMap()))
    private val timedOut = CabinSignal.Unavailable(CabinSignal.Unavailable.Cause.TimedOut)

    private fun collectFor(window: Duration, upstream: Flow<CabinSignal>): List<CabinSignal> = runBlocking {
        val results = mutableListOf<CabinSignal>()
        withTimeoutOrNull(window) { upstream.unavailableAfter(TIMEOUT).toList(results) }
        results
    }

    @Test
    fun `emits timed out when the signal stays loading`() {
        val results = collectFor(window = 300.milliseconds, upstream = flow {
            emit(CabinSignal.Loading)
            delay(10.seconds)
        })

        assertEquals(listOf(CabinSignal.Loading, timedOut), results)
    }

    @Test
    fun `does not time out when the signal becomes ready in time`() {
        val results = collectFor(window = 300.milliseconds, upstream = flow {
            emit(CabinSignal.Loading)
            delay(20.milliseconds)
            emit(ready)
            delay(10.seconds)
        })

        assertEquals(listOf(CabinSignal.Loading, ready), results)
    }

    @Test
    fun `recovers to ready after timing out`() {
        val results = collectFor(window = 400.milliseconds, upstream = flow {
            emit(CabinSignal.Loading)
            delay(200.milliseconds)
            emit(ready)
            delay(10.seconds)
        })

        assertEquals(listOf(CabinSignal.Loading, timedOut, ready), results)
    }

    @Test
    fun `does not time out when the signal is already unavailable`() {
        val unknown = CabinSignal.Unavailable(CabinSignal.Unavailable.Cause.Unknown(Sensor.DOOR_POSITION))
        val results = collectFor(window = 300.milliseconds, upstream = flow {
            emit(CabinSignal.Loading)
            emit(unknown)
            delay(10.seconds)
        })

        assertEquals(listOf(CabinSignal.Loading, unknown), results)
    }

    private companion object {
        val TIMEOUT = 100.milliseconds
    }
}
