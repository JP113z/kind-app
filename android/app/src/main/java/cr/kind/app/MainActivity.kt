package cr.kind.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/**
 * Única pantalla de la app: un WebView que muestra www/index.html.
 *
 * Los archivos se sirven con WebViewAssetLoader (https://appassets.androidplatform.net)
 * y no con file://, porque desde file:// el navegador bloquea fetch() por CORS
 * y no se podrían leer las frases.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var loader: WebViewAssetLoader

    private val reqNotifications = 7001

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        KindNotifications.ensureChannel(this)

        loader = WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = findViewById(R.id.webview)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.setBackgroundColor(getColor(R.color.kind_cream))
        web.addJavascriptInterface(WebBridge(this), "KindNative")

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)
        }

        web.loadUrl("https://appassets.androidplatform.net/index.html")
    }

    override fun onResume() {
        super.onResume()
        // por si cambió el día o el usuario tocó permisos desde ajustes
        KindWidget.updateAll(this)
        KindAlarms.reschedule(this)
    }

    /** Pide el permiso de notificaciones (Android 13+ lo exige en tiempo de ejecución). */
    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), reqNotifications)
        } else {
            notifyWeb()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == reqNotifications) notifyWeb()
    }

    private fun notifyWeb() {
        web.evaluateJavascript(
            "window.kindOnPermissionResult && window.kindOnPermissionResult();", null
        )
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
