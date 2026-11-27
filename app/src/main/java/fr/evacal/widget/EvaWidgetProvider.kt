package fr.evacal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import kotlin.concurrent.thread

class EvaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refreshAsync(context) { redraw(context, manager, ids) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        redraw(context, manager, intArrayOf(id)) // cache only, no network
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            refreshAsync(context) { redrawAll(context) }
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun refreshAsync(context: Context, then: () -> Unit) {
        val pending = goAsync()
        thread {
            try {
                Store.refresh(context)
                then()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "fr.evacal.widget.REFRESH"

        fun redrawAll(context: Context) {
            val m = AppWidgetManager.getInstance(context)
            redraw(context, m, m.getAppWidgetIds(ComponentName(context, EvaWidgetProvider::class.java)))
        }

        private fun redraw(context: Context, manager: AppWidgetManager, ids: IntArray) {
            for (id in ids) manager.updateAppWidget(id, views(context, manager.getAppWidgetOptions(id)))
        }

        private fun dp(o: Bundle, key: String, def: Int): Int = o.getInt(key, 0).takeIf { it > 0 } ?: def

        private fun views(context: Context, o: Bundle): RemoteViews {
            val minW = dp(o, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minH = dp(o, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
            val maxW = dp(o, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW).coerceAtLeast(minW)
            val maxH = dp(o, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH).coerceAtLeast(minH)
            val portrait = one(context, minW, maxH)
            if (Build.VERSION.SDK_INT < 31 || (minW == maxW && minH == maxH)) return portrait
            return RemoteViews(one(context, maxW, minH), portrait)
        }

        private fun one(context: Context, wDp: Int, hDp: Int): RemoteViews {
            val link = Store.link(context)
            val data = Store.cached(context)
            val message = if (link == null) "Ouvre EVA Widget pour coller ton lien" else "Données indisponibles"
            val density = context.resources.displayMetrics.density
            val corner = if (Build.VERSION.SDK_INT >= 31) {
                context.resources.getDimension(android.R.dimen.system_app_widget_background_radius) / density
            } else {
                16f
            }
            val plan = plan(wDp.toFloat(), hDp.toFloat(), data)
            val bmp = Renderer.render(plan, data, message, Renderer.pxPerDp(density, wDp.toFloat(), hDp.toFloat()), corner)
            val rv = RemoteViews(context.packageName, R.layout.widget)
            rv.setImageViewBitmap(R.id.image, bmp)
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val open = if (link == null) {
                PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), flags)
            } else {
                PendingIntent.getActivity(context, 0, Intent(Intent.ACTION_VIEW, Uri.parse(Link.origin(link))), flags)
            }
            rv.setOnClickPendingIntent(R.id.image, open)
            val refresh = Intent(context, EvaWidgetProvider::class.java).setAction(ACTION_REFRESH)
            rv.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(context, 1, refresh, flags))
            return rv
        }
    }
}
