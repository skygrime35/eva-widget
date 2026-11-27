package fr.evacal.widget

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed class Fetch {
    data class Ok(val json: String, val data: WidgetData) : Fetch()
    object NotFound : Fetch()
    object Failure : Fetch()
}

/** The configured link and the last good response, plus the HTTP fetch. Never logs the link. */
object Store {
    private const val PREFS = "eva_widget"
    private const val K_LINK = "link"
    private const val K_JSON = "json"
    private const val K_AT = "fetched_at"
    private const val TIMEOUT_MS = 4000 // connect + read stay under the 10 s goAsync budget
    private const val MAX_BYTES = 256 * 1024

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun link(ctx: Context): String? = prefs(ctx).getString(K_LINK, null)

    fun cached(ctx: Context): WidgetData? = prefs(ctx).getString(K_JSON, null)?.let(Parser::parse)

    /** Epoch millis of the last successful fetch, or 0. */
    fun fetchedAt(ctx: Context): Long = prefs(ctx).getLong(K_AT, 0L)

    /** Saves a new link together with its first response; replaces any previous cache. */
    fun saveLink(ctx: Context, link: String, ok: Fetch.Ok) {
        prefs(ctx).edit().putString(K_LINK, link).putString(K_JSON, ok.json).putLong(K_AT, System.currentTimeMillis()).apply()
    }

    private fun saveOk(ctx: Context, ok: Fetch.Ok) {
        prefs(ctx).edit().putString(K_JSON, ok.json).putLong(K_AT, System.currentTimeMillis()).apply()
    }

    private fun clearCache(ctx: Context) {
        prefs(ctx).edit().remove(K_JSON).remove(K_AT).apply()
    }

    /** Refetches with the saved link. A 404 (revoked link) drops the cache; other failures keep it. */
    fun refresh(ctx: Context): Fetch? {
        val link = link(ctx) ?: return null
        val r = fetch(link)
        when (r) {
            is Fetch.Ok -> saveOk(ctx, r)
            Fetch.NotFound -> clearCache(ctx)
            Fetch.Failure -> Unit
        }
        return r
    }

    fun fetch(link: String): Fetch {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(Link.dataUrl(link)).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            when (conn.responseCode) {
                200 -> {
                    val text = conn.inputStream.use { it.readBytes() }
                    if (text.size > MAX_BYTES) return Fetch.Failure
                    val json = String(text, Charsets.UTF_8)
                    val data = Parser.parse(json) ?: return Fetch.Failure
                    Fetch.Ok(json, data)
                }
                404 -> Fetch.NotFound
                else -> Fetch.Failure
            }
        } catch (e: IOException) {
            Fetch.Failure
        } catch (e: RuntimeException) {
            Fetch.Failure
        } finally {
            conn?.disconnect()
        }
    }
}
