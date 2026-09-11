package cr.kind.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Suena la alarma: mostramos la frase y volvemos a programar para mañana. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            KindAlarms.ACTION_DAILY -> {
                if (KindPrefs.isEnabled(context)) KindNotifications.showToday(context)
            }
            KindAlarms.ACTION_MIDNIGHT -> {
                // frase nueva: hay que repintar el widget
            }
        }
        KindWidget.updateAll(context)
        KindAlarms.reschedule(context)
    }
}
