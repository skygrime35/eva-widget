package fr.evacal.widget

import java.time.DateTimeException
import java.time.LocalDate

/** Link handling: `https://<host>/widget/<token>`, optionally followed by `/` or `/data`. */
object Link {
    private val RE = Regex("^(?i:https)://([^/\\s?#@]+)/widget/([^/\\s?#]+?)(?:/data)?/?$")

    /** Returns the canonical link (no trailing slash, no `/data`) or null when the input is not acceptable. */
    fun normalize(input: String): String? {
        val m = RE.matchEntire(input.trim()) ?: return null
        return "https://${m.groupValues[1]}/widget/${m.groupValues[2]}"
    }

    /** Scheme and host of a normalized link, without the token path. */
    fun origin(link: String): String = link.substringBefore("/widget/")

    fun dataUrl(link: String): String = "$link/data"
}

/** French date formatting from the string parts of venue wall-clock time; no time zone is ever involved. */
object FrDate {
    private val DAYS_SHORT = listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.")
    private val DAYS_LONG = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
    private val MONTHS_SHORT = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.")
    private val MONTHS_LONG = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
    private val DT = Regex("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2})")
    private val D = Regex("^(\\d{4})-(\\d{2})-(\\d{2})")

    /** "mar. 1 déc. 19:20" or, when [long], "mardi 1 décembre 19:20". Null if unparseable. */
    fun dateTime(startLocal: String, long: Boolean): String? {
        val m = DT.find(startLocal) ?: return null
        val date = date(m.groupValues[1], m.groupValues[2], m.groupValues[3]) ?: return null
        val dow = date.dayOfWeek.value - 1
        val mon = date.monthValue - 1
        val day = if (long) DAYS_LONG[dow] else DAYS_SHORT[dow]
        val month = if (long) MONTHS_LONG[mon] else MONTHS_SHORT[mon]
        return "$day ${date.dayOfMonth} $month ${m.groupValues[4]}:${m.groupValues[5]}"
    }

    /** "1 déc. 19:20": date and time without the weekday, for narrow areas. Null if unparseable. */
    fun compact(startLocal: String): String? = dateTime(startLocal, false)?.substringAfter(' ')

    /** "9 déc." from "2026-12-09". Null if unparseable. */
    fun day(isoDate: String): String? {
        val m = D.find(isoDate) ?: return null
        val date = date(m.groupValues[1], m.groupValues[2], m.groupValues[3]) ?: return null
        return "${date.dayOfMonth} ${MONTHS_SHORT[date.monthValue - 1]}"
    }

    private fun date(y: String, mo: String, d: String): LocalDate? = try {
        LocalDate.of(y.toInt(), mo.toInt(), d.toInt())
    } catch (e: DateTimeException) {
        null
    }
}
