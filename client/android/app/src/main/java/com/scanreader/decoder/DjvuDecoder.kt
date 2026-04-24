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

    fun getPageCount(contentResolver: ContentResolver, cacheDir: File, uri: Uri): Int {
        if (!isAvailable) return 1
        return try {
            withLocalFile(contentResolver, cacheDir, uri, "djvu_count.djv") { file ->
                DjvuBook(file.absolutePath).use { it.pageCount() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read DjVu page count: ${e.message}")
            1
        }
    }

    /**
     * Renders [pageNum] (0-based) of the DjVu file at [uri] to JPEG bytes.
     * If [uri] is already a local file URI the native decoder opens it directly;
     * otherwise the content is copied to a temp file first.
     */
    fun renderPage(contentResolver: ContentResolver, cacheDir: File, uri: Uri, pageNum: Int): ByteArray {
        if (!isAvailable) throw UnsupportedOperationException(
            "DjVu is not available on this device/ABI. Only armeabi-v7a is currently supported."
        )
        return withLocalFile(contentResolver, cacheDir, uri, "djvu_tmp.djv") { file ->
            DjvuBook(file.absolutePath).use { book -> book.renderPage(pageNum) }
        }
    }

    /**
     * If [uri] is a `file://` URI, runs [block] with the file directly (no copy).
     * Otherwise copies the content to [tmpName] in [cacheDir], runs [block], then deletes it.
     */
    private fun <T> withLocalFile(
        contentResolver: ContentResolver,
        cacheDir: File,
        uri: Uri,
        tmpName: String,
        block: (File) -> T
    ): T {
        if (uri.scheme == "file") {
            return block(File(uri.path!!))
        }
        val tmpFile = File(cacheDir, tmpName)
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                tmpFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw Exception("Cannot open DjVu file: $uri")
            block(tmpFile)
        } finally {
            tmpFile.delete()
        }
    }
}
