// Package must exactly match the original flow-reader Java package so JNI symbol names resolve.
package com.veve.flowreader.model.impl.djvu

internal class DjvuBook(path: String) : AutoCloseable {

    val bookId: Long = openBook(path)

    private external fun openBook(path: String): Long
    private external fun getNumberOfPages(bookId: Long): Int
    private external fun close(bookId: Long): Int

    fun pageCount(): Int = getNumberOfPages(bookId)

    fun renderPage(pageNum: Int): ByteArray =
        DjvuBookPage.getNativeBytes(bookId, pageNum)

    override fun close() { close(bookId) }
}
