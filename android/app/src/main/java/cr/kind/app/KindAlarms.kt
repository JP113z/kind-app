package cr.kind.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Dos alarmas repetidas a mano (no usamos setRepeating porque es inexacta):
 *   · DAILY    -> a la hora elegida, manda la notificación
 *   · MIDNIGHT -> a las 00:00, refresca el widget con la frase del nuevo día
 * Cada vez que una suena, se vuelve a programar para el día siguiente.
 */
object KindAlarms {

    const val ACTION_DAILY = "cr.kind.app.action.DAILY"
    const val ACTION_MIDNIGHT = "cr.kind.app.action.MIDNIGHT"

    private const val RC_DAILY = 100
    private const val RC_MIDNIGHT = 101

    private fun manager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun canScheduleExact(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) manager(context).canScheduleExactAlarms()
        else true

    /** Próxima ocurrencia de hora:minuto (hoy si todavía no pasó, si no mañana). */
    private fun nextTrigger(hour: Int, minute: Int): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun schedule(context: Context, action: String, requestCode: Int, at: Long) {
        val am = manager(context)
        val pi = pending(context, action, requestCode)
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                // sin permiso de alarmas exactas puede atrasarse unos minutos
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** Deja las alarmas en el estado que corresponde según la configuración. */
    fun reschedule(context: Context) {
        val am = manager(context)

        if (KindPrefs.isEnabled(context)) {
            schedule(
                context, ACTION_DAILY, RC_DAILY,
                nextTrigger(KindPrefs.hour(context), KindPrefs.minute(context))
            )
        } else {
            am.cancel(pending(context, ACTION_DAILY, RC_DAILY))
        }

        // el widget cambia de frase a la medianoche, esté o no activada la notificación
        schedule(context, ACTION_MIDNIGHT, RC_MIDNIGHT, nextTrigger(0, 0))
    }
}
