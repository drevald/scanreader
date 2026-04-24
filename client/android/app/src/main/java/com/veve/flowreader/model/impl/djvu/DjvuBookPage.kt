package com.veve.flowreader.model.impl.djvu

internal class DjvuBookPage {
    companion object {
        @JvmStatic external fun getNativeBytes(bookId: Long, pageNumber: Int): ByteArray
        @JvmStatic external fun getNativeWidth(bookId: Long, pageNumber: Int): Int
        @JvmStatic external fun getNativeHeight(bookId: Long, pageNumber: Int): Int
    }
}
