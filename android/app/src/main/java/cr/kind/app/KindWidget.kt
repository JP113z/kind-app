package cr.kind.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/**
 * El widget de Kind: la tarjeta del día en la pantalla de inicio.
 * La imagen ya trae la frase escrita; al tocarla se abre la app.
 */
class KindWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onEnabled(context: Context) {
        // primer widget colocado: aseguramos la alarma que lo refresca cada día
        KindAlarms.reschedule(context)
    }

    companion object {

        /** Repinta todos los widgets colocados (tras cambiar de día, boot, etc.). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, KindWidget::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quote)
            val quote = KindQuotes.ofToday(context)

            val bitmap = quote?.let { KindImages.fromAssets(context, it.image, 540) }
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.widget_image, KindImages.rounded(bitmap))
                views.setViewVisibility(R.id.widget_image, View.VISIBLE)
                views.setViewVisibility(R.id.widget_fallback, View.GONE)
                views.setContentDescription(R.id.widget_image, quote.text)
            } else {
                views.setViewVisibility(R.id.widget_image, View.GONE)
                views.setViewVisibility(R.id.widget_fallback, View.VISIBLE)
            }

            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            manager.updateAppWidget(id, views)
        }
    }
}
