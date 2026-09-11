package cr.kind.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** La notificación diaria con la frase de Tiger. */
object KindNotifications {

    private const val CHANNEL_ID = "kind_daily"
    private const val NOTIF_ID = 1001

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_description)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Muestra la frase de hoy. Si no hay permiso, no hace nada. */
    fun showToday(context: Context) {
        if (!hasPermission(context)) return
        val quote = KindQuotes.ofToday(context) ?: return
        ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val card = KindImages.fromAssets(context, quote.image, 512)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.kind_green))
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(quote.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (card != null) builder.setLargeIcon(card)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
        } catch (e: SecurityException) {
            // el permiso se revocó entre el chequeo y el envío: no pasa nada
        }
    }
}
