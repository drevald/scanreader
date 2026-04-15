package com.scanreader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class JsonRpcClient(private val baseUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val idCounter = AtomicInteger(0)

    suspend fun <T> call(
        method: String,
        params: JsonObject,
        deserializer: DeserializationStrategy<T>
    ): T = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("method", JsonPrimitive(method))
            put("params", params)
            put("id", JsonPrimitive(idCounter.incrementAndGet()))
        }

        val request = Request.Builder()
            .url("$baseUrl/rpc")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val responseText = client.newCall(request).execute().use { it.body!!.string() }
        val responseObj = json.parseToJsonElement(responseText).jsonObject

        responseObj["error"]?.let { error ->
            val msg = error.jsonObject["message"]?.jsonPrimitive?.content ?: "Unknown RPC error"
            throw RpcException(msg)
        }

        val result = responseObj["result"] as? JsonElement
            ?: throw RpcException("Missing result field")
        json.decodeFromJsonElement(deserializer, result)
    }

    suspend fun uploadPageImage(bookId: String, pageNum: Int, imageBytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "page.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/pages/$bookId/$pageNum")
                .post(requestBody)
                .build()

            val responseText = client.newCall(request).execute().use { it.body!!.string() }
            val responseObj = json.parseToJsonElement(responseText).jsonObject
            responseObj["image_id"]?.jsonPrimitive?.content
                ?: throw RpcException("Missing image_id in upload response")
        }
}

class RpcException(message: String) : Exception(message)
