package com.scanreader.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.scanreader.model.BlockType
import com.scanreader.model.ContentBlock
import com.scanreader.model.ReaderSettings
import com.scanreader.model.TextAlignment
import com.scanreader.model.PageData

/** Text mode: renders OCR text + pictures as a readable document. */
@Composable
fun TextReaderView(pageData: PageData, settings: ReaderSettings) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = settings.margins.dp)
    ) {
        items(pageData.blocks) { block ->
            when (block.type) {
                BlockType.PICTURE -> AsyncImage(
                    model = block.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                else -> TextBlock(block = block, settings = settings)
            }
        }
    }
}

@Composable
fun TextBlock(block: ContentBlock, settings: ReaderSettings) {
    val text = block.text ?: return
    Text(
        text = text,
        fontSize = settings.fontSize.sp,
        lineHeight = (settings.fontSize * settings.lineSpacing).sp,
        textAlign = when (settings.textAlignment) {
            TextAlignment.LEFT -> TextAlign.Left
            TextAlignment.CENTER -> TextAlign.Center
            TextAlignment.JUSTIFY -> TextAlign.Justify
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
