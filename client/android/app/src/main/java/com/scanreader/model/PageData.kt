package com.scanreader.model

import kotlinx.serialization.Serializable

@Serializable
data class PageData(
    val bookId: String,
    val pageNum: Int,
    val width: Int,
    val height: Int,
    val blocks: List<ContentBlock>
)

@Serializable
data class ContentBlock(
    val id: String,
    val type: BlockType,
    val bbox: BoundingBox,
    val text: String? = null,
    val words: List<Word> = emptyList(),
    val imageUrl: String? = null
)

@Serializable
data class Word(
    val bbox: BoundingBox,
    val text: String? = null,
    val baseline: Int? = null
)

@Serializable
data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

enum class BlockType { TEXT, PICTURE, HEADING, TABLE, FOOTER, REFERENCE, FORMULA }
