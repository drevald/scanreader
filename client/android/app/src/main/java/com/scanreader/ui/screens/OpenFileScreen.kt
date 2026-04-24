package com.scanreader.ui.screens

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.scanreader.decoder.DjvuDecoder
import com.scanreader.model.Book
import com.scanreader.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import java.util.UUID

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun OpenFileScreen(uri: Uri, navController: NavController) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                // Detect format from MIME type first, fall back to extension
                val mimeType = try { context.contentResolver.getType(uri) } catch (_: SecurityException) { null }
                val uriStr = uri.toString().lowercase()
                val format = when {
                    mimeType == "application/pdf" -> BookFormat.PDF
                    mimeType?.contains("djvu", ignoreCase = true) == true -> BookFormat.DJVU
                    uriStr.endsWith(".djvu") || uriStr.endsWith(".djv") -> BookFormat.DJVU
                    uriStr.endsWith(".pdf") -> BookFormat.PDF
                    else -> BookFormat.RASTER
                }

                // Content URIs from external apps (Yandex Disk, MediaDocuments, etc.) require
                // an active permission grant that may not survive past this composable's scope.
                // Copy to our own cache now, so the reader always works with a local file.
                val localUri: Uri = if (uri.scheme == "content") {
                    val ext = when (format) {
                        BookFormat.PDF -> "pdf"
                        BookFormat.DJVU -> "djv"
                        BookFormat.RASTER -> "img"
                    }
                    val cacheFile = File(context.cacheDir, "book_open.$ext")
                    val stream = try {
                        context.contentResolver.openInputStream(uri)
                    } catch (_: SecurityException) {
                        error("Cannot open this file. Open it using the '+' button inside the app instead of sharing from another app.")
                    }
                    stream?.use { input -> cacheFile.outputStream().use { input.copyTo(it) } }
                        ?: error("Cannot read file — the source app did not grant read access.")
                    Uri.fromFile(cacheFile)
                } else {
                    uri
                }

                val totalPages = when (format) {
                    BookFormat.PDF -> try {
                        context.contentResolver.openFileDescriptor(localUri, "r")?.use { fd ->
                            PdfRenderer(fd).use { it.pageCount }
                        } ?: 1
                    } catch (_: Exception) { 1 }
                    BookFormat.DJVU -> DjvuDecoder.getPageCount(
                        contentResolver = context.contentResolver,
                        cacheDir = context.cacheDir,
                        uri = localUri
                    )
                    BookFormat.RASTER -> 1
                }

                val fileName = uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.substringAfterLast('%')
                    ?: "Document"

                Book(
                    id = UUID.randomUUID().toString(),
                    title = fileName,
                    filePath = localUri.toString(),
                    format = format,
                    totalPages = totalPages
                )
            }
        }

        result.onSuccess { book ->
            val encoded = URLEncoder.encode(json.encodeToString(book), "UTF-8")
            navController.navigate("reader/$encoded") {
                popUpTo("open") { inclusive = true }
            }
        }.onFailure { e ->
            error = e.message ?: "Failed to open file"
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (error != null) {
            Text(
                text = error!!,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
