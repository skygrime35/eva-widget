package fr.evacal.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout as TextLayout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object Colors {
    const val BG = 0xFF07060D.toInt()
    const val PANEL = 0xFF110F1D.toInt()
    const val BORDER = 0xFF2A2545.toInt()
    const val TEXT = 0xFFF3F1FB.toInt()
    const val MUTED = 0xFFA9A5C4.toInt()
    const val ACCENT = 0xFFAB8DFF.toInt()
    const val WARNING = 0xFFFBBF24.toInt()
}

/** Draws a [Plan]; it takes no decision about content, only about how to paint it. All sizes are dp. */
object Renderer {
    private const val MAX_PIXELS = 1_500_000 // about 6 MB of ARGB_8888

    /** Pixels per dp: the display density, lowered when the bitmap would exceed the RemoteViews budget. */
    fun pxPerDp(density: Float, widthDp: Float, heightDp: Float): Float {
        val px = widthDp * heightDp * density * density
        return if (px <= MAX_PIXELS) density else density * sqrt(MAX_PIXELS / px)
    }

    fun render(plan: Plan, data: WidgetData?, message: String, pxPerDp: Float, cornerDp: Float): Bitmap {
        val wPx = max(1, (plan.widthDp * pxPerDp).roundToInt())
        val hPx = max(1, (plan.heightDp * pxPerDp).roundToInt())
        val bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(wPx / plan.widthDp, hPx / plan.heightDp)
        background(c, plan, cornerDp)
        if (data == null || plan.layout == Layout.NONE) {
            drawMessage(c, plan, message)
            return bmp
        }
        plan.ringsRect?.let { data.quota?.let { q -> rings(c, plan, it, q) } }
        plan.textRect?.let { lines(c, plan, it) }
        if (plan.flagDot) flagDot(c)
        return bmp
    }

    private fun paint(color: Int, sizeDp: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sizeDp
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            textAlign = align
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun background(c: Canvas, plan: Plan, corner: Float) {
        val r = RectF(0.5f, 0.5f, plan.widthDp - 0.5f, plan.heightDp - 0.5f)
        c.drawRoundRect(r, corner, corner, fill(Colors.BG))
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Colors.BORDER; style = Paint.Style.STROKE; strokeWidth = 1f
        }
        c.drawRoundRect(r, corner, corner, border)
    }

    private fun drawMessage(c: Canvas, plan: Plan, message: String) {
        val pad = 10f
        val w = (plan.widthDp - 2 * pad).toInt()
        val availH = plan.heightDp - 2 * Dim.vpad(plan.heightDp)
        val tp = paint(Colors.MUTED, 12f)
        val lineH = tp.fontMetrics.let { it.descent - it.ascent }
        val maxLines = max(1, (availH / lineH).toInt())
        val layout = StaticLayout.Builder.obtain(message, 0, message.length, tp, w)
            .setAlignment(TextLayout.Alignment.ALIGN_CENTER)
            .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build()
        c.save()
        c.translate(pad, (plan.heightDp - layout.height) / 2f)
        layout.draw(c)
        c.restore()
    }

    private fun flagDot(c: Canvas) {
        c.drawCircle(9f, 9f, 5.5f, fill(Colors.WARNING))
        c.drawText("!", 9f, 12.2f, paint(Colors.BG, 9f, bold = true, align = Paint.Align.CENTER))
    }

    private fun rings(c: Canvas, plan: Plan, rect: Rect, q: Quota) {
        val d = plan.ringD
        val cellW = rect.w / 2f
        val blockH = d + if (plan.showLabels) Dim.LABEL_H else 0f
        val top = rect.y + if (plan.layout == Layout.SIDE) (rect.h - blockH) / 2f else 0f
        val items = listOf(
            Triple(q.peak, if (plan.longLabels) "Heures pleines" else "Pleines", 0),
            Triple(q.offPeak, if (plan.longLabels) "Heures creuses" else "Creuses", 1),
        )
        for ((a, label, i) in items) {
            val cx = rect.x + cellW * i + cellW / 2f
            ring(c, plan, cx, top + d / 2f, d, a)
            if (plan.showLabels) {
                val lp = paint(Colors.MUTED, 11f, align = Paint.Align.CENTER)
                c.drawText(clip(label, lp, cellW - 2f), cx, top + d + Dim.LABEL_H - 3f, lp)
            }
        }
    }

    private fun ring(c: Canvas, plan: Plan, cx: Float, cy: Float, d: Float, a: Allowance) {
        val stroke = (d * 0.11f).coerceIn(4f, 8f)
        val r = d / 2f - stroke / 2f
        c.drawCircle(cx, cy, r - stroke / 2f, fill(Colors.PANEL))
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Colors.BORDER; style = Paint.Style.STROKE; strokeWidth = stroke
        }
        c.drawCircle(cx, cy, r, track)
        if (a.left > 0 && a.total > 0) {
            val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Colors.ACCENT; style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND
            }
            val sweep = 360f * min(1f, a.left.toFloat() / a.total)
            c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, sweep, false, arc)
        }
        val n = (d * if (plan.showSur) 0.36f else 0.4f).coerceIn(12f, 26f)
        val np = paint(Colors.TEXT, n, bold = true, align = Paint.Align.CENTER)
        val capN = n * 0.72f
        if (plan.showSur) {
            val capS = 11f * 0.72f
            val top = cy - (capN + 3f + capS) / 2f
            c.drawText(a.left.toString(), cx, top + capN, np)
            c.drawText("sur ${a.total}", cx, top + capN + 3f + capS, paint(Colors.MUTED, 11f, align = Paint.Align.CENTER))
        } else {
            c.drawText(a.left.toString(), cx, cy + capN / 2f, np)
        }
    }

    private fun lines(c: Canvas, plan: Plan, rect: Rect) {
        var y = rect.y + if (plan.layout == Layout.SIDE) (rect.h - plan.contentHeight) / 2f else 0f
        plan.lines.forEachIndexed { i, l ->
            if (i > 0) y += l.gapBefore
            line(c, l, rect.x, y, rect.w)
            y += l.height
        }
    }

    /** Shortens [text] with an ellipsis so that it fits [width]. */
    private fun clip(text: String, p: TextPaint, width: Float): String {
        if (p.measureText(text) <= width) return text
        val ell = "…"
        val n = p.breakText(text, true, max(0f, width - p.measureText(ell)), null)
        return text.substring(0, n).trimEnd(' ', '·') + ell
    }

    private fun line(c: Canvas, l: Line, x: Float, y: Float, w: Float) {
        when (l.kind) {
            Kind.FLAG -> {
                c.drawCircle(x + 3f, y + 7.5f, 3f, fill(Colors.WARNING))
                val p = paint(Colors.WARNING, 11f, bold = true)
                c.drawText(clip(l.text, p, w - 9f), x + 9f, y + 11.5f, p)
            }
            Kind.LEVEL -> {
                val p = paint(Colors.TEXT, 12f, bold = true)
                val rp = paint(Colors.MUTED, 12f, align = Paint.Align.RIGHT)
                val rw = l.right?.let { rp.measureText(it) + 6f } ?: 0f
                c.drawText(clip(l.text, p, w - rw), x, y + 12f, p)
                l.right?.let { c.drawText(it, x + w, y + 12f, rp) }
                l.fraction?.let { f ->
                    val bar = RectF(x, y + l.height - 4f, x + w, y + l.height)
                    c.drawRoundRect(bar, 2f, 2f, fill(Colors.BORDER))
                    if (f > 0f) c.drawRoundRect(RectF(x, bar.top, x + max(4f, w * f), bar.bottom), 2f, 2f, fill(Colors.ACCENT))
                }
            }
            Kind.NEXT_DATE -> {
                val p = paint(Colors.TEXT, 13f, bold = true)
                c.drawText(clip(l.text, p, w), x, y + 13f, p)
            }
            Kind.NEXT_GAME -> {
                val p = paint(Colors.ACCENT, 12f)
                c.drawText(clip(l.text, p, w), x, y + 12f, p)
            }
            Kind.QUOTA_MISSING, Kind.EMPTY, Kind.FOLLOWING, Kind.PLAYERS, Kind.ENDS, Kind.RENEWAL -> {
                val p = paint(Colors.MUTED, 12f)
                c.drawText(clip(l.text, p, w), x, y + 12f, p)
            }
        }
    }
}
