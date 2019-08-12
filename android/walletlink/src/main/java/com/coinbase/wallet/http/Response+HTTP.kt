package com.coinbase.wallet.http

import com.coinbase.wallet.core.util.JSON
import okhttp3.Response

/**
 * Convert an okhttp response to a CBHTTP response
 */
inline fun <reified T : Any> Response.asHTTPResponse(): HTTPResponse<T> {
    val json = body()?.string() ?: throw HTTPException.UnabelToDeserialize
    val result = JSON.fromJsonString<T>(json) ?: throw HTTPException.UnabelToDeserialize

    val rspHeaders = headers()
    val headersMap = (0 until rspHeaders.size())
        .map { rspHeaders.name(it) to rspHeaders.value(it) }
        .toMap()

    return HTTPResponse(body = result, headers = headersMap)
}
