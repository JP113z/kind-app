package cr.kind.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Al reiniciar el teléfono (o al actualizar la app) las alarmas se pierden:
 * las volvemos a poner y refrescamos el widget.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        KindAlarms.reschedule(context)
        KindWidget.updateAll(context)
    }
}
