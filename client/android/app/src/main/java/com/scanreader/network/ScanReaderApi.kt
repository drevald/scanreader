package com.scanreader.network

import com.scanreader.model.PageData
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer

class ScanReaderApi(baseUrl: String) {

    private val rpc = JsonRpcClient(baseUrl)

    suspend fun uploadPageImage(bookId: String, pageNum: Int, imageBytes: ByteArray): String =
        rpc.uploadPageImage(bookId, pageNum, imageBytes)

    suspend fun processPage(bookId: String, pageNum: Int, imageId: String): PageData =
        rpc.call(
            method = "process_page",
            params = buildJsonObject {
                put("book_id", JsonPrimitive(bookId))
                put("page_num", JsonPrimitive(pageNum))
                put("image_id", JsonPrimitive(imageId))
            },
            deserializer = serializer()
        )
}
