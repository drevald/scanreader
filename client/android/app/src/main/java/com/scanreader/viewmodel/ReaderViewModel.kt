package com.scanreader.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanreader.decoder.DjvuDecoder
import com.scanreader.model.Book
import com.scanreader.model.BookFormat
import com.scanreader.model.PageData
import com.scanreader.model.ReaderSettings
import com.scanreader.model.ViewMode
import com.scanreader.network.ScanReaderApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    // TODO: inject via factory / settings
    private val api = ScanReaderApi(baseUrl = "http://10.0.2.2:8000")

    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _pageData = MutableStateFlow<PageData?>(null)
    val pageData: StateFlow<PageData?> = _pageData

    private val _settings = MutableStateFlow(ReaderSettings())
    val settings: StateFlow<ReaderSettings> = _settings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun openBook(book: Book) {
        _currentBook.value = book
        _currentPage.value = book.lastPage
        loadPage(book.lastPage)
    }

    fun goToPage(pageNum: Int) {
        val book = _currentBook.value ?: return
        val clamped = pageNum.coerceIn(0, book.totalPages - 1)
        _currentPage.value = clamped
        loadPage(clamped)
    }

    fun nextPage() = goToPage(_currentPage.value + 1)
    fun prevPage() = goToPage(_currentPage.value - 1)

    fun updateSettings(settings: ReaderSettings) {
        _settings.value = settings
    }

    fun setViewMode(mode: ViewMode) {
        _settings.value = _settings.value.copy(viewMode = mode)
    }

    private fun loadPage(pageNum: Int) {
        val book = _currentBook.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val imageBytes = renderPageToJpeg(book, pageNum)
                val imageId = api.uploadPageImage(book.id, pageNum, imageBytes)
                _pageData.value = api.processPage(book.id, pageNum, imageId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun renderPageToJpeg(book: Book, pageNum: Int): ByteArray =
        withContext(Dispatchers.IO) {
            val app = getApplication<Application>()
            val uri = Uri.parse(book.filePath)
            when (book.format) {
                BookFormat.RASTER -> {
                    app.contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: throw Exception("Cannot read image: ${book.filePath}")
                }
                BookFormat.PDF -> renderPdfPageToJpeg(app, uri, pageNum)
                BookFormat.DJVU -> DjvuDecoder.renderPage(
                    contentResolver = app.contentResolver,
                    cacheDir = app.cacheDir,
                    uri = uri,
                    pageNum = pageNum
                )
            }
        }

    private fun renderPdfPageToJpeg(app: Application, uri: Uri, pageNum: Int): ByteArray {
        val fd = app.contentResolver.openFileDescriptor(uri, "r")
            ?: throw Exception("Cannot open PDF: $uri")
        return fd.use {
            PdfRenderer(fd).use { renderer ->
                if (pageNum >= renderer.pageCount)
                    throw Exception("Page $pageNum out of range (${renderer.pageCount} pages)")
                renderer.openPage(pageNum).use { page ->
                    val bm = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val out = ByteArrayOutputStream()
                    bm.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    bm.recycle()
                    out.toByteArray()
                }
            }
        }
    }
}
