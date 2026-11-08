package com.pony.safe_kids.data.vehicle

import com.pony.safe_kids.domain.model.LocationSnapshot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

internal sealed interface Reading {
    data class Known(val value: Boolean) : Reading
    data object Unknown : Reading
}

internal fun readingOf(value: Boolean?): Reading =
    if (value == null) Reading.Unknown else Reading.Known(value)

internal sealed interface AreaEvent<out L> {
    data class Supported<L>(val locations: Set<L>) : AreaEvent<L>
    data class Updated<L>(val location: L, val reading: Reading) : AreaEvent<L>
    data class Read<L>(val location: L, val reading: Reading) : AreaEvent<L>
    data object CapabilityFailed : AreaEvent<Nothing>
}

internal data class AreaState<L>(
    val locations: Set<L>? = null,
    val readings: Map<L, Reading> = emptyMap(),
    val capabilityFailed: Boolean = false,
) {
    fun apply(event: AreaEvent<L>): AreaState<L> = when (event) {
        is AreaEvent.Supported -> copy(locations = event.locations)
        is AreaEvent.Updated -> copy(readings = readings + (event.location to event.reading))
        is AreaEvent.Read -> if (event.location in readings) this else copy(readings = readings + (event.location to event.reading))
        AreaEvent.CapabilityFailed -> copy(capabilityFailed = true)
    }

    fun snapshot(): LocationSnapshot<L> {
        if (capabilityFailed) return LocationSnapshot.Failed
        val locations = locations ?: return LocationSnapshot.Pending
        val values = mutableMapOf<L, Boolean>()
        val unknown = mutableSetOf<L>()
        for (location in locations) {
            when (val reading = readings[location]) {
                null -> return LocationSnapshot.Pending
                Reading.Unknown -> unknown += location
                is Reading.Known -> values[location] = reading.value
            }
        }
        return if (unknown.isEmpty()) LocationSnapshot.Ready(values) else LocationSnapshot.Unknown(unknown)
    }
}

internal interface AreaSource<A, L> {
    val name: String

    fun checkCapabilities(onComplete: (Collection<A>) -> Unit, onFailed: (Exception) -> Unit)

    fun toLocation(area: A): L?

    fun subscribe(area: A, onReading: (Reading) -> Unit, onError: (Exception) -> Unit): () -> Unit

    fun read(area: A, onReading: (Reading) -> Unit, onError: (Exception) -> Unit)
}

internal fun <A, L> areaSignalFlow(
    source: AreaSource<A, L>,
    onError: (message: String, e: Throwable) -> Unit,
): Flow<LocationSnapshot<L>> = callbackFlow {
    val unsubscribes = mutableListOf<() -> Unit>()

    awaitCapabilities(source)
        .onSuccess { areas ->
            val supported = areas.mapNotNull { area -> source.toLocation(area)?.let { area to it } }
            trySend(AreaEvent.Supported(supported.map { (_, location) -> location }.toSet()))
            supported.forEach { (area, location) ->
                try {
                    unsubscribes += source.subscribe(
                        area,
                        { reading -> trySend(AreaEvent.Updated(location, reading)) },
                        { e -> onError("${source.name} $location failed", e) },
                    )
                    source.read(
                        area,
                        { reading -> trySend(AreaEvent.Read(location, reading)) },
                        { e -> onError("${source.name} $location get failed", e) },
                    )
                } catch (e: Exception) {
                    onError("${source.name} $location subscribe failed", e)
                }
            }
        }
        .onFailure { e ->
            onError("${source.name} capability failed", e)
            trySend(AreaEvent.CapabilityFailed)
        }

    awaitClose { unsubscribes.forEach { it() } }
}
    .buffer(Channel.UNLIMITED)
    .runningFold(AreaState<L>(), AreaState<L>::apply)
    .map { it.snapshot() }
    .distinctUntilChanged()

private suspend fun <A> awaitCapabilities(source: AreaSource<A, *>): Result<Collection<A>> =
    suspendCancellableCoroutine { continuation ->
        val answered = AtomicBoolean(false)
        fun answer(result: Result<Collection<A>>) {
            if (answered.compareAndSet(false, true)) continuation.resume(result)
        }
        source.checkCapabilities(
            onComplete = { areas -> answer(Result.success(areas)) },
            onFailed = { e -> answer(Result.failure(e)) },
        )
    }
