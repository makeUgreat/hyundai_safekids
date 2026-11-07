package com.pony.safe_kids.data.vehicle

import com.pony.safe_kids.domain.model.LocationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class AreaStateTest {

    private val on = Reading.Known(true)
    private val off = Reading.Known(false)

    private fun stateOf(vararg events: AreaEvent<String>): AreaState<String> =
        events.fold(AreaState()) { state, event -> state.apply(event) }

    @Test
    fun `readingOf maps null to Unknown and a value to Known`() {
        assertEquals(Reading.Unknown, readingOf(null))
        assertEquals(Reading.Known(true), readingOf(true))
        assertEquals(Reading.Known(false), readingOf(false))
    }

    @Test
    fun `is pending before supported locations are known`() {
        assertEquals(LocationSnapshot.Pending, stateOf(AreaEvent.Updated("a", on)).snapshot())
    }

    @Test
    fun `is pending until every supported location has a reading`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a", "b")),
            AreaEvent.Updated("a", on),
        )

        assertEquals(LocationSnapshot.Pending, state.snapshot())
    }

    @Test
    fun `is ready when every supported location has a known reading`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a", "b")),
            AreaEvent.Updated("a", on),
            AreaEvent.Read("b", off),
        )

        assertEquals(LocationSnapshot.Ready(mapOf("a" to true, "b" to false)), state.snapshot())
    }

    @Test
    fun `is unknown with the locations whose reading is unknown`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a", "b")),
            AreaEvent.Updated("a", Reading.Unknown),
            AreaEvent.Updated("b", on),
        )

        assertEquals(LocationSnapshot.Unknown(setOf("a")), state.snapshot())
    }

    @Test
    fun `is pending rather than unknown while some location has no reading`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a", "b")),
            AreaEvent.Updated("a", Reading.Unknown),
        )

        assertEquals(LocationSnapshot.Pending, state.snapshot())
    }

    @Test
    fun `read does not overwrite a reading from the listener`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a")),
            AreaEvent.Updated("a", on),
            AreaEvent.Read("a", off),
        )

        assertEquals(LocationSnapshot.Ready(mapOf("a" to true)), state.snapshot())
    }

    @Test
    fun `listener overwrites a reading from read`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a")),
            AreaEvent.Read("a", off),
            AreaEvent.Updated("a", on),
        )

        assertEquals(LocationSnapshot.Ready(mapOf("a" to true)), state.snapshot())
    }

    @Test
    fun `listener turns a known reading back into unknown`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a")),
            AreaEvent.Updated("a", on),
            AreaEvent.Updated("a", Reading.Unknown),
        )

        assertEquals(LocationSnapshot.Unknown(setOf("a")), state.snapshot())
    }

    @Test
    fun `ignores readings of unsupported locations`() {
        val state = stateOf(
            AreaEvent.Supported(setOf("a")),
            AreaEvent.Updated("a", on),
            AreaEvent.Updated("z", off),
        )

        assertEquals(LocationSnapshot.Ready(mapOf("a" to true)), state.snapshot())
    }
}
