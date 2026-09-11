package cr.kind.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface

/**
 * Puente entre la interfaz web y Android. Todo lo que app.js llama como
 * window.KindNative.<algo>() entra por acá.
 */
class WebBridge(private val activity: Activity) {

    @JavascriptInterface
    fun getSettings(): String = KindPrefs.asJson(activity)

    @JavascriptInterface
    fun saveSettings(enabled: Boolean, hour: Int, minute: Int) {
        KindPrefs.save(activity, enabled, hour, minute)
        KindAlarms.reschedule(activity)
    }

    @JavascriptInterface
    fun hasNotificationPermission(): Boolean = KindNotifications.hasPermission(activity)

    @JavascriptInterface
    fun requestNotificationPermission() {
        activity.runOnUiThread { (activity as? MainActivity)?.askNotificationPermission() }
    }

    @JavascriptInterface
    fun canScheduleExactAlarms(): Boolean = KindAlarms.canScheduleExact(activity)

    @JavascriptInterface
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        activity.runOnUiThread {
            try {
                activity.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:" + activity.packageName))
                )
            } catch (e: Exception) {
                activity.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    @JavascriptInterface
    fun sendTestNotification() {
        KindNotifications.showToday(activity)
    }

    @JavascriptInterface
    fun canPinWidget(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(activity).isRequestPinAppWidgetSupported

    /** Atajo para agregar el widget sin salir de la app (si el launcher lo permite). */
    @JavascriptInterface
    fun requestPinWidget() {
        if (!canPinWidget()) return
        activity.runOnUiThread {
            AppWidgetManager.getInstance(activity).requestPinAppWidget(
                ComponentName(activity, KindWidget::class.java), null, null
            )
        }
    }
}
