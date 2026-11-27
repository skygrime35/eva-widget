package fr.evacal.widget

import org.junit.Assert.*
import org.junit.Test

class FormatTest {
    private val base = "https://example.org/widget/abc123"

    @Test fun normalizesVariants() {
        assertEquals(base, Link.normalize(base))
        assertEquals(base, Link.normalize("  $base/  "))
        assertEquals(base, Link.normalize("$base/data"))
        assertEquals(base, Link.normalize("$base/data/"))
        assertEquals(base, Link.normalize("HTTPS://example.org/widget/abc123"))
        assertEquals("https://example.org:8443/widget/t", Link.normalize("https://example.org:8443/widget/t"))
    }

    @Test fun rejectsBadLinks() {
        for (s in listOf(
            "", "abc", "http://example.org/widget/abc123", "https://example.org/widget/", "https://example.org/widget",
            "https://example.org/other/abc", "https://example.org/widget/abc?x=1", "https://example.org/widget/a/b",
            "ftp://example.org/widget/abc", "https://user@example.org/widget/abc",
        )) assertNull(s, Link.normalize(s))
    }

    @Test fun originHasNoToken() {
        assertEquals("https://example.org", Link.origin(base))
        assertFalse(Link.origin(base).contains("abc123"))
        assertEquals("$base/data", Link.dataUrl(base))
    }

    @Test fun shortDate() {
        assertEquals("mar. 1 déc. 19:20", FrDate.dateTime("2026-12-01T19:20", false))
        assertEquals("jeu. 3 déc. 20:40", FrDate.dateTime("2026-12-03T20:40", false))
    }

    @Test fun longDate() {
        assertEquals("mardi 1 décembre 19:20", FrDate.dateTime("2026-12-01T19:20", true))
        assertEquals("dimanche 28 février 09:05", FrDate.dateTime("2027-02-28T09:05", true))
    }

    @Test fun allMonthsAndWeekdays() {
        val shortM = (1..12).map { FrDate.dateTime("2026-%02d-15T10:00".format(it), false)!!.split(" ")[2] }
        assertEquals(listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."), shortM)
        val days = (1..7).map { FrDate.dateTime("2026-11-%02dT10:00".format(it + 22), false)!!.substringBefore(" ") }
        assertEquals(listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."), days)
    }

    @Test fun noTimeZoneDrift() {
        val saved = java.util.TimeZone.getDefault()
        try {
            for (id in listOf("UTC", "Pacific/Kiritimati", "America/Los_Angeles", "Europe/Paris")) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(id))
                assertEquals("mar. 1 déc. 00:30", FrDate.dateTime("2026-12-01T00:30", false))
                assertEquals("jeu. 31 déc. 23:59", FrDate.dateTime("2026-12-31T23:59", false))
            }
        } finally { java.util.TimeZone.setDefault(saved) }
    }

    @Test fun unparseable() {
        assertNull(FrDate.dateTime("garbage", false))
        assertNull(FrDate.dateTime("2026-13-01T10:00", false))
        assertEquals("9 déc.", FrDate.day("2026-12-09"))
        assertNull(FrDate.day("x"))
    }
}
