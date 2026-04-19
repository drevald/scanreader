package com.scanreader.decoder

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.veve.flowreader.model.impl.djvu.DjvuBook
import java.io.File

private const val TAG = "DjvuDecoder"

object DjvuDecoder {

    val isAvailable: Boolean by lazy {
        try {
            System.loadLibrary("native-lib")
            Log.i(TAG, "native-lib loaded successfully")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "native-lib unavailable (${e.message}) — DjVu not supported on this ABI")
            false
        }
    }

    /**
     * Renders [pageNum] (0-based) of the DjVu file at [uri] to JPEG bytes.
     * The content URI is copied to a temp file first, because the native DjVuLibre
     * decoder needs a real filesystem path rather than a content-scheme URI.
     */
    fun renderPage(contentResolver: ContentResolver, cacheDir: File, uri: Uri, pageNum: Int): ByteArray {
        if (!isAvailable) throw UnsupportedOperationException(
            "DjVu is not available on this device/ABI. Only armeabi-v7a is currently supported."
        )

        val tmpFile = File(cacheDir, "djvu_tmp.djv")
        contentResolver.openInputStream(uri)?.use { input ->
            tmpFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw Exception("Cannot open DjVu file: $uri")

        return try {
            DjvuBook(tmpFile.absolutePath).use { book -> book.renderPage(pageNum) }
        } finally {
            tmpFile.delete()
        }
    }
}
