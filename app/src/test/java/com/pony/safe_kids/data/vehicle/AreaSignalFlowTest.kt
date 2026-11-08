package com.pony.safe_kids.data.vehicle

import com.pony.safe_kids.domain.model.LocationSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaSignalFlowTest {

    private class FakeAreaSource : AreaSource<String, String> {
        override val name = "Fake"

        var onCapabilities: ((Collection<String>) -> Unit)? = null
        var onCapabilityFailed: ((Exception) -> Unit)? = null
        val listeners = mutableMapOf<String, (Reading) -> Unit>()
        val reads = mutableMapOf<String, (Reading) -> Unit>()
        val unsubscribed = mutableListOf<String>()
        val failingAreas = mutableSetOf<String>()
        val errors = mutableListOf<String>()

        override fun checkCapabilities(onComplete: (Collection<String>) -> Unit, onFailed: (Exception) -> Unit) {
            onCapabilities = onComplete
            onCapabilityFailed = onFailed
        }

        override fun toLocation(area: String): String? = area.takeUnless { it == UNMAPPED_AREA }?.lowercase()

        override fun subscribe(area: String, onReading: (Reading) -> Unit, onError: (Exception) -> Unit): () -> Unit {
            if (area in failingAreas) throw SecurityException("denied")
            listeners[area] = onReading
            return { unsubscribed += area }
        }

        override fun read(area: String, onReading: (Reading) -> Unit, onError: (Exception) -> Unit) {
            reads[area] = onReading
        }
    }

    private fun CoroutineScope.collect(
        source: FakeAreaSource,
        results: MutableList<LocationSnapshot<String>> = mutableListOf(),
    ): Job = launch(Dispatchers.Unconfined) {
        areaSignalFlow(source) { message, _ -> source.errors += message }.toList(results)
    }

    @Test
    fun `emits pending first and ready once every location is known`() = runBlocking {
        val source = FakeAreaSource()
        val results = mutableListOf<LocationSnapshot<String>>()
        val job = collect(source, results)

        source.onCapabilities!!(listOf("A", "B"))
        source.listeners.getValue("A")(Reading.Known(true))
        source.reads.getValue("B")(Reading.Known(false))
        job.cancelAndJoin()

        assertEquals(listOf(LocationSnapshot.Pending, LocationSnapshot.Ready(mapOf("a" to true, "b" to false))), results)
    }

    @Test
    fun `skips areas without a location`() = runBlocking {
        val source = FakeAreaSource()
        val job = collect(source)

        source.onCapabilities!!(listOf("A", UNMAPPED_AREA))
        job.cancelAndJoin()

        assertEquals(setOf("A"), source.listeners.keys)
        assertEquals(setOf("A"), source.reads.keys)
    }

    @Test
    fun `unsubscribes every listener when collection is cancelled`() = runBlocking {
        val source = FakeAreaSource()
        val job = collect(source)

        source.onCapabilities!!(listOf("A", "B"))
        job.cancelAndJoin()

        assertEquals(listOf("A", "B"), source.unsubscribed)
    }

    @Test
    fun `does not subscribe when capabilities arrive after cancellation`() = runBlocking {
        val source = FakeAreaSource()
        val job = collect(source)

        job.cancelAndJoin()
        source.onCapabilities!!(listOf("A"))

        assertTrue(source.listeners.isEmpty())
        assertTrue(source.reads.isEmpty())
    }

    @Test
    fun `reports a failing subscription and keeps subscribing the other areas`() = runBlocking {
        val source = FakeAreaSource().apply { failingAreas += "A" }
        val job = collect(source)

        source.onCapabilities!!(listOf("A", "B"))
        job.cancelAndJoin()

        assertEquals(listOf("Fake a subscribe failed"), source.errors)
        assertEquals(setOf("B"), source.listeners.keys)
        assertEquals(listOf("B"), source.unsubscribed)
    }

    @Test
    fun `ignores a second capability response`() = runBlocking {
        val source = FakeAreaSource()
        val job = collect(source)

        source.onCapabilities!!(listOf("A"))
        source.onCapabilities!!(listOf("A", "B"))
        source.onCapabilityFailed!!(Exception("late"))
        job.cancelAndJoin()

        assertEquals(setOf("A"), source.listeners.keys)
        assertTrue(source.errors.isEmpty())
    }

    @Test
    fun `reports capability failure and emits failed`() = runBlocking {
        val source = FakeAreaSource()
        val results = mutableListOf<LocationSnapshot<String>>()
        val job = collect(source, results)

        source.onCapabilityFailed!!(Exception("boom"))
        job.cancelAndJoin()

        assertEquals(listOf("Fake capability failed"), source.errors)
        assertEquals(listOf(LocationSnapshot.Pending, LocationSnapshot.Failed), results)
    }

    private companion object {
        const val UNMAPPED_AREA = "UNMAPPED"
    }
}
