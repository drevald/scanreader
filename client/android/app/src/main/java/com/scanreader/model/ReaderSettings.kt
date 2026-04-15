package com.scanreader.model

data class ReaderSettings(
    val viewMode: ViewMode = ViewMode.ORIGINAL,
    val fontSize: Int = 16,
    val fontFamily: String = "serif",
    val margins: Int = 16,
    val lineSpacing: Float = 1.5f,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val textAlignment: TextAlignment = TextAlignment.JUSTIFY
)

enum class ViewMode { ORIGINAL, REFLOW, TEXT }
enum class ReaderTheme { LIGHT, SEPIA, DARK }
enum class TextAlignment { LEFT, CENTER, JUSTIFY }
