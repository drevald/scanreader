package com.veve.flowreader.model.impl.djvu

// Static native methods — companion + @JvmStatic produces the same JNI symbol as Java's
// `private static native byte[] getNativeBytes(...)`.
internal class DjvuBookPage {
    companion object {
        @JvmStatic
        external fun getNativeBytes(bookId: Long, pageNumber: Int): ByteArray
    }
}
