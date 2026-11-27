package fr.evacal.widget

import kotlin.math.min

data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)

enum class Layout { NONE, SIDE, TOP }

enum class Kind { QUOTA_MISSING, FLAG, LEVEL, NEXT_DATE, NEXT_GAME, EMPTY, FOLLOWING, PLAYERS, ENDS, RENEWAL }

/** One text line of the right/lower area, already formatted. [right] and [fraction] only apply to LEVEL. */
data class Line(
    val kind: Kind,
    val text: String,
    val height: Float,
    val gapBefore: Float,
    val right: String? = null,
    val fraction: Float? = null,
)

/**
 * Everything the renderer needs: what is shown, with its text and its rectangles (all in dp).
 * [lines] is a prefix of the priority-ordered list that fits [textRect].
 */
data class Plan(
    val widthDp: Float,
    val heightDp: Float,
    val layout: Layout,
    val ringsRect: Rect? = null,
    val ringD: Float = 0f,
    val showSur: Boolean = false,
    val showLabels: Boolean = false,
    val longLabels: Boolean = false,
    val textRect: Rect? = null,
    val flagDot: Boolean = false,
    val lines: List<Line> = emptyList(),
    val contentHeight: Float = 0f,
) {
    val score: Int get() = lines.size
}

object Dim {
    const val HPAD = 8f
    const val GAP = 6f
    const val LINE = 16f
    const val DATE_H = 17f
    const val LEVEL_BAR_H = 20f
    const val LABEL_H = 14f
    const val MIN_D = 30f
    const val MAX_D = 76f
    const val TOP_D = 44f
    const val MIN_TEXT_W = 84f
    const val SUR_MIN_D = 42f
    const val LONG_LABEL_CELL = 80f
    const val DATE_CHAR_W = 6.3f // 13dp bold
    const val LINE_CHAR_W = 5.6f // 12dp regular
    const val GAME_MIN_W = 30f // room wanted for the game name after a following booking's date

    /** Vertical padding grows smoothly with the height so that the usable height never shrinks. */
    fun vpad(h: Float): Float = (4f + (h - 40f) / 6f).coerceIn(4f, 10f)
}

fun plan(widthDp: Float, heightDp: Float, data: WidgetData?): Plan {
    if (data == null) return Plan(widthDp, heightDp, Layout.NONE)
    val side = planSide(widthDp, heightDp, data)
    if (data.quota == null) return side
    val top = planTop(widthDp, heightDp, data) ?: return side
    return if (top.score > side.score || (top.score == side.score && widthDp < 200f)) top else side
}

private fun flagText(flag: String): String = when (flag) {
    "reconnect" -> "Compte à reconnecter"
    "stale" -> "Données anciennes"
    else -> "Données à vérifier"
}

private fun allLines(data: WidgetData, textWidth: Float): List<Line> {
    val out = ArrayList<Line>()
    if (data.quota == null) out.add(Line(Kind.QUOTA_MISSING, "Quota indisponible", Dim.LINE, 0f))
    data.flag?.let { out.add(Line(Kind.FLAG, flagText(it), Dim.LINE, 4f)) }
    data.level?.let { lv ->
        val pct = lv.progressionPct
        out.add(
            Line(
                Kind.LEVEL, "Niveau ${lv.level}",
                if (pct != null) Dim.LEVEL_BAR_H else Dim.LINE, 4f,
                right = pct?.let { "$it %" }, fraction = pct?.let { it / 100f },
            ),
        )
    }
    val b = data.bookings
    if (b.isEmpty()) {
        out.add(Line(Kind.EMPTY, "Aucune réservation à venir", Dim.LINE, 6f))
    } else {
        val first = b[0]
        val long = FrDate.dateTime(first.startLocal, true)
        val useLong = long != null && long.length * Dim.DATE_CHAR_W <= textWidth
        val short = FrDate.dateTime(first.startLocal, false)
        val nextText = when {
            long == null || short == null -> first.startLocal
            useLong -> long
            short.length * Dim.DATE_CHAR_W <= textWidth -> short
            else -> FrDate.compact(first.startLocal) ?: short
        }
        out.add(Line(Kind.NEXT_DATE, nextText, Dim.DATE_H, 6f))
        if (first.game.isNotBlank()) out.add(Line(Kind.NEXT_GAME, first.game, Dim.LINE, 0f))
        b.drop(1).forEachIndexed { i, bk ->
            val short = FrDate.dateTime(bk.startLocal, false)
            val d = when {
                short == null -> bk.startLocal
                short.length * Dim.LINE_CHAR_W + Dim.GAME_MIN_W <= textWidth -> short
                else -> FrDate.compact(bk.startLocal) ?: short
            }
            out.add(Line(Kind.FOLLOWING, if (bk.game.isBlank()) d else "$d · ${bk.game}", Dim.LINE, if (i == 0) 8f else 2f))
        }
        if (first.players.isNotEmpty()) out.add(Line(Kind.PLAYERS, "Avec ${first.players.joinToString(", ")}", Dim.LINE, 8f))
    }
    data.quota?.daysLeft?.let { n ->
        out.add(Line(Kind.ENDS, if (n <= 0) "Dernier jour du quota" else "Fin du quota dans $n j", Dim.LINE, 4f))
    }
    data.renewsAt?.let { FrDate.day(it) }?.let { out.add(Line(Kind.RENEWAL, "Renouvellement $it", Dim.LINE, 4f)) }
    return out
}

/** Greedy prefix of [all] that fits [height]; returns the lines and their total height. */
private fun fill(all: List<Line>, height: Float): Pair<List<Line>, Float> {
    var used = 0f
    val taken = ArrayList<Line>()
    for (l in all) {
        val need = used + (if (taken.isEmpty()) 0f else l.gapBefore) + l.height
        if (need > height) break
        used = need
        taken.add(l)
    }
    return taken to used
}

private fun finish(base: Plan, data: WidgetData, all: List<Line>, textHeight: Float): Plan {
    val (lines, used) = if (base.textRect != null) fill(all, textHeight) else emptyList<Line>() to 0f
    return base.copy(lines = lines, contentHeight = used, flagDot = data.flag != null && lines.none { it.kind == Kind.FLAG })
}

private fun planSide(w: Float, h: Float, data: WidgetData): Plan {
    val vp = Dim.vpad(h)
    val availH = h - 2 * vp
    val availW = w - 2 * Dim.HPAD
    val hasRings = data.quota != null
    var blockW = 0f
    var textX = Dim.HPAD
    var textW = availW
    if (hasRings) {
        val split = min(0.40f * w, 150f)
        val tw = availW - split - 8f
        if (tw >= Dim.MIN_TEXT_W) {
            blockW = split; textW = tw; textX = Dim.HPAD + split + 8f
        } else {
            blockW = availW; textW = 0f
        }
    }
    val textRect = if (textW > 0f) Rect(textX, vp, textW, availH) else null
    var base = Plan(w, h, Layout.SIDE, textRect = textRect)
    if (hasRings) {
        val cellW = blockW / 2f
        val labels = availH - Dim.LABEL_H >= Dim.MIN_D
        val d = min(min(cellW - 4f, if (labels) availH - Dim.LABEL_H else availH), Dim.MAX_D).coerceAtLeast(12f)
        base = base.copy(
            ringsRect = Rect(Dim.HPAD, vp, blockW, availH), ringD = d, showSur = d >= Dim.SUR_MIN_D,
            showLabels = labels, longLabels = labels && cellW >= Dim.LONG_LABEL_CELL,
        )
    }
    return finish(base, data, allLines(data, textW), availH)
}

private fun planTop(w: Float, h: Float, data: WidgetData): Plan? {
    val vp = Dim.vpad(h)
    val availW = w - 2 * Dim.HPAD
    val d = min(Dim.TOP_D, (availW / 2f) - 4f)
    val ringsH = Dim.TOP_D + Dim.LABEL_H
    if (2 * vp + ringsH > h) return null
    val th = h - 2 * vp - ringsH - Dim.GAP
    val textRect = if (th >= Dim.LINE) Rect(Dim.HPAD, vp + ringsH + Dim.GAP, availW, th) else null
    val base = Plan(
        w, h, Layout.TOP,
        ringsRect = Rect(Dim.HPAD, vp, availW, ringsH), ringD = d, showSur = d >= Dim.SUR_MIN_D,
        showLabels = true, longLabels = availW / 2f >= Dim.LONG_LABEL_CELL, textRect = textRect,
    )
    return finish(base, data, allLines(data, availW), th)
}
