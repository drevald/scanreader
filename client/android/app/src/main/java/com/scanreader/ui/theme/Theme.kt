package com.scanreader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Matches the warm beige/brown palette from the Figma design
private val AppColorScheme = lightColorScheme(
    primary = Color(0xFF8B7D6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4C5B2),
    background = Color(0xFFF5F0EA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun ScanReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
