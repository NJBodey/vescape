package app.vescape.wear

import expo.modules.vescapecore.telemetry.UnitPresentation
import java.io.File
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

/** @parity /modules/vescape-core/ios/watch/WatchUnitsTests.swift */
class WatchUnitsTest {
    @Test
    fun `distance fixtures match phone and watchOS`() {
        val fixture = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "shared/fixtures/rider-units.json") }.first { it.exists() }
        val rows = JSONArray(fixture.readText())
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            for (units in listOf("km", "mi")) {
                assertEquals(row.getString(units), UnitPresentation.distance(row.getDouble("meters"), units))
            }
        }
        assertEquals("—", UnitPresentation.distance(Double.NaN, "mi"))
        assertEquals(25.0, UnitPresentation.speedFromKmh(40.2336, "mi"), 1e-10)
    }

    @Test
    fun `weather temperatures convert before rounding`() {
        assertEquals(21, UnitPresentation.wholeTemperatureFromC(21.0, "celsius"))
        assertEquals(70, UnitPresentation.wholeTemperatureFromC(21.0, "fahrenheit"))
        assertEquals(-4, UnitPresentation.wholeTemperatureFromC(-20.0, "fahrenheit"))
        assertEquals(21, UnitPresentation.wholeTemperatureFromC(21.0, "unknown"))
    }
}
