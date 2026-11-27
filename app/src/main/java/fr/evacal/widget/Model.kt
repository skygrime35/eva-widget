package fr.evacal.widget

import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

data class Allowance(val total: Int, val left: Int)

data class Quota(val peak: Allowance, val offPeak: Allowance, val daysLeft: Int?, val endsAt: String?)

data class Level(val level: Int, val progressionPct: Int?)

data class Booking(val startLocal: String, val game: String, val players: List<String>)

data class WidgetData(
    val generatedAt: String?,
    val flag: String?,
    val quota: Quota?,
    val level: Level?,
    val bookings: List<Booking>,
    val renewsAt: String?,
)

/** Tolerant parser for the JSON data contract: anything unusable becomes null or is skipped. */
object Parser {
    const val MAX_BOOKINGS = 8

    fun parse(text: String): WidgetData? {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            return null
        }
        return try {
            WidgetData(
                generatedAt = str(root, "generatedAt"),
                flag = str(root, "flag"),
                quota = quota(root.optJSONObject("quota")),
                level = level(root.optJSONObject("level")),
                bookings = bookings(root),
                renewsAt = str(root, "renewsAt"),
            )
        } catch (e: JSONException) {
            null
        }
    }

    private fun str(o: JSONObject, key: String): String? = if (o.isNull(key)) null else o.optString(key, "").ifEmpty { null }

    private fun int(o: JSONObject, key: String): Int? = if (o.isNull(key)) null else o.optDouble(key, Double.NaN).takeIf { !it.isNaN() }?.roundToInt()

    private fun allowance(o: JSONObject?): Allowance? {
        if (o == null) return null
        val total = int(o, "total") ?: return null
        val left = int(o, "left") ?: return null
        return Allowance(total, left)
    }

    private fun quota(o: JSONObject?): Quota? {
        if (o == null) return null
        val peak = allowance(o.optJSONObject("peak")) ?: return null
        val off = allowance(o.optJSONObject("offPeak")) ?: return null
        return Quota(peak, off, int(o, "daysLeft"), str(o, "endsAt"))
    }

    private fun level(o: JSONObject?): Level? {
        if (o == null) return null
        val level = int(o, "level") ?: return null
        return Level(level, int(o, "progressionPct")?.coerceIn(0, 100))
    }

    private fun bookings(root: JSONObject): List<Booking> {
        val arr = root.optJSONArray("bookings") ?: return emptyList()
        val out = ArrayList<Booking>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val start = str(o, "startLocal") ?: continue
            val players = ArrayList<String>()
            o.optJSONArray("players")?.let { p ->
                for (j in 0 until p.length()) if (!p.isNull(j)) p.optString(j, "").takeIf { it.isNotBlank() }?.let(players::add)
            }
            out.add(Booking(start, str(o, "game") ?: "", players))
            if (out.size >= MAX_BOOKINGS) break
        }
        return out
    }
}
