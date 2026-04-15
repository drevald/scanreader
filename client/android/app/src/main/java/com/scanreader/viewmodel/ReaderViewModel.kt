package com.scanreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanreader.model.Book
import com.scanreader.model.PageData
import com.scanreader.model.ReaderSettings
import com.scanreader.model.ViewMode
import com.scanreader.network.ScanReaderApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReaderViewModel : ViewModel() {

    // TODO: inject via factory
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
                // TODO: render page from DjVu/PDF to JPEG bytes using local decoder
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

    // Stub — replace with DjVu/PDF decoder from sibling project
    private fun renderPageToJpeg(book: Book, pageNum: Int): ByteArray {
        TODO("Integrate DjVu/PDF decoder from flow-reader")
    }
}
