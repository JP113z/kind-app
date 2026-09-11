package cr.kind.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/** Carga y recorte de las imágenes de las frases (viven en assets/img/quotes). */
object KindImages {

    /**
     * Decodifica una imagen de assets reduciéndola a ~maxPx de lado.
     * Importante para el widget: RemoteViews tiene un límite de tamaño y
     * mandar el PNG de 1080px completo puede hacer que no se dibuje.
     */
    fun fromAssets(context: Context, path: String, maxPx: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) {
        null
    }

    /** Esquinas redondeadas, para que la tarjeta se vea como tarjeta. */
    fun rounded(src: Bitmap, radiusRatio: Float = 0.085f): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val r = minOf(src.width, src.height) * radiusRatio
        canvas.drawRoundRect(RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), r, r, paint)
        return out
    }
}
