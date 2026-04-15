package com.scanreader.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val filePath: String,
    val format: BookFormat,
    val totalPages: Int,
    val lastPage: Int = 0
)

enum class BookFormat { DJVU, PDF, RASTER }
