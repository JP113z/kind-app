package cr.kind.app

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/** Una frase con su imagen. La imagen vive en assets (img/quotes/qNN.png). */
data class Quote(val id: String, val text: String, val image: String)

/**
 * Lee content/quotes.json desde los assets del APK — el MISMO archivo que
 * usa la interfaz web, así que nunca se desincronizan.
 */
object KindQuotes {

    @Volatile
    private var cache: List<Quote>? = null

    fun all(context: Context): List<Quote> {
        cache?.let { return it }
        val list = try {
            val json = context.assets.open("content/quotes.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = JSONObject(json).getJSONArray("quotes")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Quote(o.getString("id"), o.getString("text"), o.getString("image"))
            }
        } catch (e: Exception) {
            emptyList()
        }
        cache = list
        return list
    }

    /**
     * Índice de la frase de hoy. Es exactamente la misma fórmula que en
     * app.js (días desde 1970 en hora local, módulo la cantidad de frases),
     * para que la pantalla, el widget y la notificación coincidan siempre.
     */
    fun todayIndex(size: Int): Int {
        if (size <= 0) return 0
        val day = LocalDate.now().toEpochDay()
        return (((day % size) + size) % size).toInt()
    }

    fun ofToday(context: Context): Quote? {
        val list = all(context)
        return if (list.isEmpty()) null else list[todayIndex(list.size)]
    }
}
