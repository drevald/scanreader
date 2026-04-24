package com.scanreader.viewmodel

import android.app.Application
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanreader.decoder.DjvuDecoder
import com.scanreader.model.Book
import com.scanreader.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LibraryViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    // TODO: persist to local DB (Room)
    fun addBookFromUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val mimeType = try { app.contentResolver.getType(uri) } catch (_: SecurityException) { null }
                val uriStr = uri.toString().lowercase()
                val format = when {
                    mimeType == "application/pdf" -> BookFormat.PDF
                    mimeType?.contains("djvu", ignoreCase = true) == true -> BookFormat.DJVU
                    uriStr.endsWith(".djvu") || uriStr.endsWith(".djv") -> BookFormat.DJVU
                    uriStr.endsWith(".pdf") -> BookFormat.PDF
                    else -> BookFormat.RASTER
                }

                val ext = when (format) {
                    BookFormat.PDF -> "pdf"
                    BookFormat.DJVU -> "djv"
                    BookFormat.RASTER -> "img"
                }
                val cacheFile = File(app.cacheDir, "${UUID.randomUUID()}.$ext")
                val copied = try {
                    app.contentResolver.openInputStream(uri)
                        ?.use { input -> cacheFile.outputStream().use { input.copyTo(it) } }
                    true
                } catch (_: Exception) { false }

                if (!copied) return@withContext

                val localUri = Uri.fromFile(cacheFile)

                val totalPages = when (format) {
                    BookFormat.PDF -> try {
                        app.contentResolver.openFileDescriptor(localUri, "r")?.use { fd ->
                            PdfRenderer(fd).use { it.pageCount }
                        } ?: 1
                    } catch (_: Exception) { 1 }
                    BookFormat.DJVU -> DjvuDecoder.getPageCount(
                        contentResolver = app.contentResolver,
                        cacheDir = app.cacheDir,
                        uri = localUri
                    )
                    BookFormat.RASTER -> 1
                }

                val title = uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.substringAfterLast('%')
                    ?: "Document"

                val book = Book(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    filePath = localUri.toString(),
                    format = format,
                    totalPages = totalPages
                )
                _books.value = _books.value + book
            }
        }
    }

    fun removeBook(bookId: String) {
        _books.value = _books.value.filter { it.id != bookId }
    }
}
