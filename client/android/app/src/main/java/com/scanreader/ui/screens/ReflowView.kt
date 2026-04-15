package com.scanreader.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scanreader.model.BlockType
import com.scanreader.model.PageData
import com.scanreader.model.ReaderSettings

/**
 * Reflow view: lays out identified content blocks (text words as raster crops,
 * pictures as images) sequentially for the narrow mobile screen.
 * TODO: render actual cropped word/block bitmaps from the page image.
 */
@Composable
fun ReflowView(pageData: PageData, settings: ReaderSettings) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = settings.margins.dp)
    ) {
        items(pageData.blocks) { block ->
            when (block.type) {
                BlockType.PICTURE -> AsyncImage(
                    model = block.imageUrl, // served by server GET /images/...
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                else -> {
                    // TODO: render raster word-block crops in reading order
                    // Placeholder: show text until raster reflow is implemented
                    TextBlock(block = block, settings = settings)
                }
            }
        }
    }
}
