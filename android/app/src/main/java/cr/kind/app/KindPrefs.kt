package cr.kind.app

import android.content.Context

/** Configuración del usuario: si quiere el recordatorio y a qué hora. */
object KindPrefs {

    private const val FILE = "kind_prefs"
    private const val K_ENABLED = "enabled"
    private const val K_HOUR = "hour"
    private const val K_MINUTE = "minute"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(K_ENABLED, false)

    fun hour(context: Context): Int = prefs(context).getInt(K_HOUR, 8)

    fun minute(context: Context): Int = prefs(context).getInt(K_MINUTE, 0)

    fun save(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        prefs(context).edit()
            .putBoolean(K_ENABLED, enabled)
            .putInt(K_HOUR, hour.coerceIn(0, 23))
            .putInt(K_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun asJson(context: Context): String =
        """{"enabled":${isEnabled(context)},"hour":${hour(context)},"minute":${minute(context)}}"""
}
