// Package must exactly match the original flow-reader Java package so JNI symbol names resolve.
package com.veve.flowreader.model.impl.djvu

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import java.io.ByteArrayOutputStream

private const val TAG = "DjvuBook"

internal class DjvuBook(path: String) : AutoCloseable {

    val bookId: Long = openBook(path)

    private external fun openBook(path: String): Long
    private external fun getNumberOfPages(bookId: Long): Int
    private external fun close(bookId: Long): Int

    fun pageCount(): Int = getNumberOfPages(bookId)

    // getNativeBytes returns raw RGBA pixels (4 bytes per pixel: R, G, B, A).
    // Get width/height BEFORE rendering — ddjvu_document_get_pageinfo may report
    // zeros once page decoding has completed and the internal state has advanced.
    fun renderPage(pageNum: Int): ByteArray {
        // Query dimensions first, while the page hasn't been decoded yet
        var w = DjvuBookPage.getNativeWidth(bookId, pageNum)
        var h = DjvuBookPage.getNativeHeight(bookId, pageNum)
        Log.d(TAG, "page $pageNum: w=$w h=$h (before render)")

        val rawRgba = DjvuBookPage.getNativeBytes(bookId, pageNum)
        Log.d(TAG, "rawRgba.size=${rawRgba.size}")

        // Fallback: derive from byte array if native calls returned 0
        if (w <= 0 || h <= 0) {
            val area = rawRgba.size / 4
            w = DjvuBookPage.getNativeWidth(bookId, pageNum)
            h = DjvuBookPage.getNativeHeight(bookId, pageNum)
            Log.d(TAG, "after render: w=$w h=$h area=$area")
            if (w <= 0 && h <= 0 && area > 0) {
                // Assume A4 portrait as last resort
                w = Math.sqrt(area * 0.707).toInt()
                h = area / w
                Log.w(TAG, "using aspect-ratio fallback: w=$w h=$h")
            } else if (w <= 0 && h > 0) {
                w = area / h
            } else if (h <= 0 && w > 0) {
                h = area / w
            }
        }

        if (w <= 0 || h <= 0) throw IllegalStateException(
            "Could not determine DjVu page dimensions (w=$w h=$h rawSize=${rawRgba.size})"
        )

        val pixels = IntArray(w * h) { i ->
            val r = rawRgba[i * 4 + 0].toInt() and 0xFF
            val g = rawRgba[i * 4 + 1].toInt() and 0xFF
            val b = rawRgba[i * 4 + 2].toInt() and 0xFF
            val a = rawRgba[i * 4 + 3].toInt() and 0xFF
            Color.argb(a, r, g, b)
        }

        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bm.setPixels(pixels, 0, w, 0, 0, w, h)
        val out = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 90, out)
        bm.recycle()
        return out.toByteArray()
    }

    override fun close() { close(bookId) }
}
