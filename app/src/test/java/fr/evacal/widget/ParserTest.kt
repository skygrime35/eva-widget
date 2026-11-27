package fr.evacal.widget

import org.junit.Assert.*
import org.junit.Test

class ParserTest {
    @Test fun parsesFullPayload() {
        val d = Sample.data
        assertNull(d.flag)
        assertEquals(Allowance(6, 4), d.quota!!.peak)
        assertEquals(Allowance(2, 0), d.quota!!.offPeak)
        assertEquals(12, d.quota!!.daysLeft)
        assertEquals(Level(37, 62), d.level)
        assertEquals(8, d.bookings.size)
        assertEquals(listOf("Roro", "Kaz"), d.bookings[0].players)
        assertEquals("2026-12-09", d.renewsAt)
    }

    @Test fun nullsBecomeNulls() {
        val d = Parser.parse("""{"flag":"reconnect","quota":null,"level":null,"bookings":[],"renewsAt":null}""")!!
        assertEquals("reconnect", d.flag)
        assertNull(d.quota); assertNull(d.level); assertNull(d.renewsAt)
        assertTrue(d.bookings.isEmpty())
    }

    @Test fun missingFieldsAreTolerated() {
        val d = Parser.parse("{}")!!
        assertNull(d.quota); assertNull(d.level); assertTrue(d.bookings.isEmpty())
    }

    @Test fun nullProgressionKeepsLevel() {
        val d = Parser.parse("""{"level":{"level":3,"progressionPct":null}}""")!!
        assertEquals(Level(3, null), d.level)
    }

    @Test fun incompleteQuotaIsNull() {
        assertNull(Parser.parse("""{"quota":{"peak":{"total":6,"left":4}}}""")!!.quota)
    }

    @Test fun badBookingsAreSkippedAndCapped() {
        val many = (1..12).joinToString(",") { """{"startLocal":"2026-12-%02dT10:00","game":"G"}""".format(it) }
        val d = Parser.parse("""{"bookings":[{"game":"x"},null,$many]}""")!!
        assertEquals(8, d.bookings.size)
        assertEquals("2026-12-01T10:00", d.bookings[0].startLocal)
    }

    @Test fun garbageIsNull() {
        assertNull(Parser.parse("not json"))
        assertNull(Parser.parse("[]"))
        assertNull(Parser.parse(""))
    }
}
