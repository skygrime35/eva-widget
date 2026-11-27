package fr.evacal.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/** Launcher screen and widget configuration screen: paste the link, validate it, refresh on demand. */
class MainActivity : Activity() {
    private lateinit var field: EditText
    private lateinit var status: TextView
    private lateinit var updated: TextView
    private lateinit var save: Button
    private lateinit var refresh: Button
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The link is a secret: keep it out of screenshots and the recents thumbnail.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        }
        buildUi()
        showUpdated()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun button(s: String, filled: Boolean) = Button(this).apply {
        text = s; isAllCaps = false
        setTextColor(if (filled) Colors.BG else Colors.TEXT)
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(if (filled) Colors.ACCENT else Colors.PANEL)
            setStroke(dp(1), if (filled) Colors.ACCENT else Colors.BORDER)
        }
    }

    private fun buildUi() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(24))
        }
        col.addView(text("EVA Widget", 24f, Colors.TEXT, bold = true))
        col.addView(
            text(
                "Colle ici le lien secret du widget. Tu le trouves dans l'application web : " +
                    "Réglages > Widget > Créer le lien.\n\n" +
                    "Ensuite, appuie longuement sur l'écran d'accueil, puis Widgets > EVA Widget. " +
                    "Agrandis le widget pour voir plus de réservations.",
                14f, Colors.MUTED,
            ),
        )
        field = EditText(this).apply {
            hint = "https://…/widget/…"
            setHintTextColor(Colors.MUTED); setTextColor(Colors.TEXT)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(true)
            Store.link(this@MainActivity)?.let { setText(it) }
        }
        col.addView(field, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        status = text("", 14f, Color.parseColor("#f87171"))
        col.addView(status)
        save = button("Enregistrer", true).apply { setOnClickListener { onSave() } }
        col.addView(save, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        updated = text("", 13f, Colors.MUTED)
        col.addView(updated, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        refresh = button("Actualiser maintenant", false).apply { setOnClickListener { onRefresh() } }
        col.addView(refresh, LinearLayout.LayoutParams(-1, -2))
        setContentView(ScrollView(this).apply { setBackgroundColor(Colors.BG); addView(col) })
    }

    private fun showUpdated() {
        val at = Store.fetchedAt(this)
        updated.text = if (at == 0L) "Dernière mise à jour : jamais"
        else "Dernière mise à jour : " + SimpleDateFormat("d MMM HH:mm", Locale.FRANCE).format(Date(at))
    }

    private fun busy(b: Boolean) {
        save.isEnabled = !b; refresh.isEnabled = !b
    }

    private fun show(msg: String, ok: Boolean) {
        status.setTextColor(if (ok) Color.parseColor("#86efac") else Color.parseColor("#f87171"))
        status.text = msg
    }

    private fun onSave() {
        val link = Link.normalize(field.text.toString())
        if (link == null) {
            show("Ce n'est pas un lien https://…/widget/… valide.", false)
            return
        }
        busy(true); show("Vérification…", true)
        thread {
            val r = Store.fetch(link)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy(false)
                when (r) {
                    is Fetch.Ok -> {
                        Store.saveLink(this, link, r)
                        EvaWidgetProvider.redrawAll(this)
                        field.setText(link)
                        show("Lien enregistré.", true)
                        showUpdated()
                    }
                    Fetch.NotFound -> show("Lien invalide ou supprimé.", false)
                    Fetch.Failure -> show("Impossible de joindre le serveur.", false)
                }
            }
        }
    }

    private fun onRefresh() {
        if (Store.link(this) == null) {
            show("Enregistre d'abord un lien.", false)
            return
        }
        busy(true); show("Actualisation…", true)
        thread {
            val r = Store.refresh(this)
            EvaWidgetProvider.redrawAll(this)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                busy(false)
                when (r) {
                    is Fetch.Ok -> show("Données à jour.", true)
                    Fetch.NotFound -> show("Lien invalide ou supprimé.", false)
                    else -> show("Impossible de joindre le serveur.", false)
                }
                showUpdated()
            }
        }
    }
}
