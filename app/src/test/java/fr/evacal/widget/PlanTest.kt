package fr.evacal.widget

import org.junit.Assert.*
import org.junit.Test

class PlanTest {
    private val d = Sample.data
    private fun kinds(p: Plan) = p.lines.map { it.kind }

    @Test fun noDataIsMessageOnly() {
        val p = plan(250f, 110f, null)
        assertEquals(Layout.NONE, p.layout)
        assertTrue(p.lines.isEmpty())
    }

    @Test fun tinyShowsRingsOnly() {
        val p = plan(110f, 40f, d)
        assertNotNull(p.ringsRect)
        assertTrue(p.lines.isEmpty())
        assertFalse(p.showSur); assertFalse(p.showLabels)
    }

    @Test fun wideShortShowsLevelAndNextBooking() {
        val p = plan(250f, 110f, d)
        assertEquals(Layout.SIDE, p.layout)
        assertEquals(Kind.LEVEL, p.lines[0].kind)
        assertEquals(Kind.NEXT_DATE, p.lines[1].kind)
        assertEquals(Kind.NEXT_GAME, p.lines[2].kind)
    }

    @Test fun tallNarrowStacksUnderRings() {
        val p = plan(110f, 250f, d)
        assertEquals(Layout.TOP, p.layout)
        assertTrue(p.lines.size >= 5)
        assertTrue(p.showLabels)
        assertEquals("1 déc. 19:20", p.lines.first { it.kind == Kind.NEXT_DATE }.text)
    }

    @Test fun largeShowsEverything() {
        val p = plan(320f, 320f, d)
        assertEquals(Layout.SIDE, p.layout)
        val k = kinds(p)
        assertEquals(7, k.count { it == Kind.FOLLOWING })
        assertTrue(k.containsAll(listOf(Kind.LEVEL, Kind.NEXT_DATE, Kind.PLAYERS, Kind.ENDS, Kind.RENEWAL)))
        assertEquals("mardi 1 décembre 19:20", p.lines.first { it.kind == Kind.NEXT_DATE }.text)
        assertEquals("Avec Roro, Kaz", p.lines.first { it.kind == Kind.PLAYERS }.text)
    }

    @Test fun flagIsAlwaysVisible() {
        val f = d.copy(flag = "reconnect")
        val big = plan(250f, 110f, f)
        assertEquals(Kind.FLAG, big.lines[0].kind)
        assertEquals("Compte à reconnecter", big.lines[0].text)
        assertFalse(big.flagDot)
        assertTrue(plan(110f, 40f, f).flagDot)
        assertEquals("Données anciennes", plan(250f, 110f, d.copy(flag = "stale")).lines[0].text)
    }

    @Test fun missingQuotaAndEmptyBookings() {
        val p = plan(250f, 110f, d.copy(quota = null, bookings = emptyList()))
        assertNull(p.ringsRect)
        assertEquals(Kind.QUOTA_MISSING, p.lines[0].kind)
        assertTrue(kinds(p).contains(Kind.EMPTY))
    }

    @Test fun lastDayWording() {
        val q = d.quota!!.copy(daysLeft = 0)
        val p = plan(320f, 320f, d.copy(quota = q))
        assertEquals("Dernier jour du quota", p.lines.first { it.kind == Kind.ENDS }.text)
    }

    @Test fun linesAlwaysFitTheirRect() {
        for (w in 110..400 step 10) for (h in 40..420 step 10) {
            val p = plan(w.toFloat(), h.toFloat(), d.copy(flag = "stale"))
            val r = p.textRect
            if (r == null) { assertTrue(p.lines.isEmpty()); continue }
            assertTrue("$w x $h", p.contentHeight <= r.h + 0.001f)
            assertTrue(r.x + r.w <= w + 0.001f && r.y + r.h <= h + 0.001f)
            p.ringsRect?.let { assertTrue("rings $w x $h", it.y + it.h <= h && it.x + it.w <= w + 0.001f && p.ringD <= it.h) }
        }
    }

    @Test fun biggerNeverShowsLess() {
        for (data in listOf(d, d.copy(flag = "reconnect"), d.copy(level = null), d.copy(quota = null))) {
            for (w in 110..400 step 5) for (h in 40..420 step 5) {
                val s = plan(w.toFloat(), h.toFloat(), data).score
                assertTrue("w+5 at $w x $h", plan(w + 5f, h.toFloat(), data).score >= s)
                assertTrue("h+5 at $w x $h", plan(w.toFloat(), h + 5f, data).score >= s)
            }
        }
    }
}
