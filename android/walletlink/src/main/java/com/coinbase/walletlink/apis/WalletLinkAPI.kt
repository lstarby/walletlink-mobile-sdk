package com.coinbase.walletlink.apis

import com.coinbase.wallet.core.extensions.base64EncodedString
import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.dtos.ServerRequestDTO
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * HTTP basic authentication credentials
 *
 * @property username HTTP basic authentication username
 * @property password HTTP basic authentication password
 */
data class Credentials(val username: String, val password: String) {
    /**
     * Properly encoded and formatted HTTP basic authentication header value
     */
    val basicAuth: String?
        get() {
            val credentialString = "$username:$password"
            val data = credentialString.toByteArray(Charsets.UTF_8)

            return "Basic ${data.base64EncodedString()}"
        }
}

data class HTTPService(val url: URL) {
    companion object
}

fun URL.appendingPathComponent(component: String): URL {
    val filePieces = mutableListOf(this.file)
    val path = if (component.startsWith("/")) {
        component.substring(1)
    } else {
        component
    }

    if (!file.endsWith("/")) {
        filePieces.add("/")
    }

    filePieces.add(path)

    return URL(protocol, host, port, filePieces.joinToString(separator = ""), null)
}

sealed class HTTPException(msg: String): Exception(msg) {
    object UnabelToDeserialize: HTTPException("Unable to deserialize response")
}

class QueryString {
    private var query = ""

    fun add(name: String, value: String) {
        query += "&"
        encode(name, value)
    }

    private fun encode(name: String, value: String) {
        try {
            query += URLEncoder.encode(name, "UTF-8")
            query += "="
            query += URLEncoder.encode(value, "UTF-8")
        } catch (ex: UnsupportedEncodingException) {
            throw RuntimeException("Broken VM does not support UTF-8")
        }
    }

    override fun toString(): String = query
}

object HTTP {
    const val kDefaultTimeout: Long = 15
    val client = OkHttpClient.Builder().connectTimeout(kDefaultTimeout, TimeUnit.SECONDS).build()
    val schedulers = Schedulers.io()

    inline fun <reified T> get(
        service: HTTPService,
        path: String,
        credentials: Credentials? = null,
        parameters: Map<String, String>? = null,
        headers: Map<String, String>? = null
    ): Single<T> {
        var builder = Request.Builder()

        headers?.let { headers -> headers.forEach { builder = builder.header(it.key, it.value) } }
        credentials?.basicAuth?.let { builder = builder.header("Authorization", it) }

        var url = service.url.appendingPathComponent(path)
        if (parameters != null) {
            val queryString = QueryString()
            parameters.forEach { queryString.add(it.key, it.value) }
            url = URL("$url?$queryString")
        }

        builder = builder.url(url)

        val request = builder.build()

        return Single
            .create<T> { emitter ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { emitter.onError(e) }

                    override fun onResponse(call: Call, response: Response) {
                        val json = response.body()?.string() ?:
                            return emitter.onError(HTTPException.UnabelToDeserialize)

                        val result = JSON.fromJsonString<T>(json) ?:
                            return emitter.onError(HTTPException.UnabelToDeserialize)

                        emitter.onSuccess(result)
                    }
                })
            }
            .subscribeOn(schedulers)
    }
}

internal class WalletLinkAPI { // TODO (private val http: WalletLinkHTTP) {
    fun markEventAsSeen(eventId: String, sessionId: String, secret: String, url: URL): Single<Unit> {
        // TODO: return http.markEventAsSeen(eventId, sessionId, secret, url)
        TODO()
    }

    fun getUnseenEvents(sessionId: String, secret: String, url: URL): Single<List<ServerRequestDTO>> {
        // TODO return http.getUnseenEvents(sessionId, secret, url)
        TODO()
    }
}

internal interface WalletLinkHTTP {
    /**
     * Mark a given event as seen
     *
     * @param eventId The event ID
     * @param sessionId The session ID
     * @param secret The session secret
     *
     * @return A Single wrapping a ServerRequestDTO
     */
    //@POST("")
    fun markEventAsSeen(eventId: String, sessionId: String, secret: String, url: URL): Single<Unit>

//    {
//        let credentials = Credentials(sessionId: sessionId, secret: secret)
//
//        return HTTP.post(
//            service: HTTPService(url: url),
//        path: "/events/\(eventId)/seen",
//        credentials: credentials
//        )
//        .asVoid()
//            .logError()
//            .catchErrorJustReturn(())
//    }

    /**
     * Fetch all unseen events
     *
     * @param sessionId The session ID
     * @param unseen If true, returns only unseen requests
     * @param sessionKey Generated session key
     *
     * @return A Single wrapping a list of encrypted host requests
     */
    //@GET("")
    fun getUnseenEvents(sessionId: String, secret: String, url: URL): Single<List<ServerRequestDTO>>
//    {
//        let credentials = Credentials(sessionId: sessionId, secret: secret)
//
//        return HTTP.get(
//            service: HTTPService(url: url),
//        path: "/events",
//        credentials: credentials,
//        parameters: ["unseen": "true"],
//        for: GetEventsDTO.self
//        )
//        .map { response in
//                response.body.events.map { event in
//                        ServerRequestDTO(
//                            sessionId: sessionId,
//                            type: .event,
//                    event: event.event,
//                    eventId: event.id,
//                    data: event.data
//                    )
//                }
//        }
//    }
}
