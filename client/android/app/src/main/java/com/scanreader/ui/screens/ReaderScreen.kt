package com.scanreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scanreader.model.Book
import com.scanreader.model.ViewMode
import com.scanreader.ui.components.BookPageHeader
import com.scanreader.ui.components.ReaderSettingsSheet
import com.scanreader.viewmodel.ReaderViewModel

@Composable
fun ReaderScreen(
    book: Book,
    onBack: () -> Unit,
    vm: ReaderViewModel = viewModel()
) {
    LaunchedEffect(book.id) { vm.openBook(book) }

    val pageData by vm.pageData.collectAsState()
    val renderedImageBytes by vm.renderedImageBytes.collectAsState()
    val settings by vm.settings.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val currentPage by vm.currentPage.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BookPageHeader(
                currentMode = settings.viewMode,
                onModeChange = { vm.setViewMode(it) },
                onFontDecrease = {
                    vm.updateSettings(settings.copy(fontSize = (settings.fontSize - 2).coerceAtLeast(10)))
                },
                onFontIncrease = {
                    vm.updateSettings(settings.copy(fontSize = (settings.fontSize + 2).coerceAtMost(36)))
                },
                onSettingsClick = { showSettings = true },
                onBack = onBack
            )
        },
        bottomBar = {
            PageNavigationBar(
                currentPage = currentPage,
                totalPages = book.totalPages,
                onPageChange = { vm.goToPage(it) },
                onPrev = { vm.prevPage() },
                onNext = { vm.nextPage() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                error != null -> Text("Error: $error")
                settings.viewMode == ViewMode.ORIGINAL && renderedImageBytes != null ->
                    OriginalPageView(imageBytes = renderedImageBytes!!)
                settings.viewMode != ViewMode.ORIGINAL && pageData != null -> {
                    val data = pageData!!
                    when (settings.viewMode) {
                        ViewMode.REFLOW -> ReflowView(pageData = data, settings = settings)
                        ViewMode.TEXT -> TextReaderView(pageData = data, settings = settings)
                        ViewMode.ORIGINAL -> {} // unreachable
                    }
                }
            }
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            settings = settings,
            onSettingsChange = { vm.updateSettings(it) },
            onDismiss = { showSettings = false }
        )
    }
}
