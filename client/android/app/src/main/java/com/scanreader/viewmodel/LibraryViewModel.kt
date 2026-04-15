package com.scanreader.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.scanreader.model.Book
import com.scanreader.model.BookFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class LibraryViewModel : ViewModel() {

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    // TODO: persist to local DB (Room)
    fun addBook(uri: Uri, title: String) {
        val format = when {
            uri.path?.endsWith(".djvu", ignoreCase = true) == true -> BookFormat.DJVU
            uri.path?.endsWith(".pdf", ignoreCase = true) == true -> BookFormat.PDF
            else -> BookFormat.RASTER
        }
        val book = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            filePath = uri.toString(),
            format = format,
            totalPages = 0 // resolved after opening
        )
        _books.value = _books.value + book
    }

    fun removeBook(bookId: String) {
        _books.value = _books.value.filter { it.id != bookId }
    }
}
