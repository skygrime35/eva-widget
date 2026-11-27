package fr.evacal.widget

import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PreviewTest {
    private val density = 2f
    private val out = File("build/outputs/widget-previews").apply { mkdirs() }

    private fun render(name: String, w: Int, h: Int, data: WidgetData?, message: String = "Données indisponibles") {
        val p = plan(w.toFloat(), h.toFloat(), data)
        val bmp = Renderer.render(p, data, message, Renderer.pxPerDp(density, w.toFloat(), h.toFloat()), 16f)
        assertEquals((w * density).toInt(), bmp.width)
        assertEquals((h * density).toInt(), bmp.height)
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        val inner = px.count { it != Colors.BG && (it ushr 24) != 0 }
        assertTrue("$name is blank", inner > 200)
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun rendersAllSizes() {
        val d = Sample.data
        for ((w, h) in listOf(110 to 40, 180 to 110, 250 to 40, 250 to 110, 250 to 180, 110 to 250, 320 to 320)) {
            render("${w}x$h", w, h, d)
        }
        render("250x110-reconnect", 250, 110, d.copy(flag = "reconnect"))
        render("110x40-reconnect", 110, 40, d.copy(flag = "reconnect"))
        render("250x110-nolink", 250, 110, null, "Ouvre EVA Widget pour coller ton lien")
        render("250x110-noquota", 250, 110, d.copy(quota = null))
    }

    @Test fun capsPixelBudget() {
        val ppd = Renderer.pxPerDp(3f, 400f, 600f)
        assertTrue(400 * ppd * 600 * ppd <= 1_500_001)
        assertEquals(2f, Renderer.pxPerDp(2f, 110f, 40f), 0f)
    }
}
