package com.coinbase.networking.websocket

import com.coinbase.networking.models.WebConnectionConnected
import com.coinbase.networking.models.WebConnectionDisconnected
import com.coinbase.networking.models.WebConnectionState
import com.coinbase.networking.models.WebIncomingData
import com.coinbase.networking.models.WebIncomingDataType
import com.coinbase.networking.models.WebIncomingText
import com.coinbase.networking.models.isConnected
import com.coinbase.walletlink.exceptions.WebSocketException
import com.coinbase.walletlink.extensions.asUnit
import com.coinbase.walletlink.extensions.takeSingle
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class WebSocket(
    private val url: String,
    private val connectionTimeout: Long = 15,
    private val minReconnectDelay: Long = 1,
    private val maxReconnectDelay: Long = 5
) : WebSocketListener() {
    private val accessQueue = ReentrantLock()
    private val incomingSubject = PublishSubject.create<WebIncomingDataType>()
    private val connectionStateSubject = ReplaySubject.create<WebConnectionState>()
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS) // heartbeat
        .retryOnConnectionFailure(false)
        .build()

    private var socket: WebSocket? = null
    private var isManualClose: Boolean = false
    private var reconnectAttempts = 0
    private var isConnected: Boolean = false

    // Observable for all incoming text or data messages
    public val incomingObservable: Observable<WebIncomingDataType> = incomingSubject.hide()

    // Observable for web socket connection state
    public val connectionStateObservable: Observable<WebConnectionState> = connectionStateSubject.hide()

    /**
     * Connect to given web socket
     *
     * @return A single indication a successful connection. Otherwise, an error is thrown.
     */
    public fun connect(): Single<Unit> {
        var isCurrentlyConnected = false

        accessQueue.withLock {
            isCurrentlyConnected = this.isConnected
            this.isManualClose = false
        }

        if (isCurrentlyConnected) {
            return Single.just(Unit)
        }

        return connectionStateObservable
            .doOnSubscribe { connectSocket() }
            .filter { it.isConnected }
            .takeSingle()
            .timeout(connectionTimeout, TimeUnit.SECONDS)
            .asUnit()
    }

    /**
    * Disconnect from websocket if connection is live
    *
    * @return A single indication connection was terminated
    */
    public fun disconnect(): Single<Unit> {
        var isCurrentlyConnected = false

        accessQueue.withLock {
            isCurrentlyConnected = this.isConnected
            this.isManualClose = true
        }

        if (!isCurrentlyConnected) {
            return Single.just(Unit)
        }

        return connectionStateObservable
            .doOnSubscribe { disconnectSocket() }
            .filter { !it.isConnected }
            .takeSingle()
            .asUnit()
    }

    /**
     * Send string-based message to server
     *
     *  @param string String-based message to send
     *
     * @return A single wrapping `Void` fired when send request completes
     */
    public fun sendString(string: String): Single<Unit> {
        if (socket?.send(string) == true) {
            return Single.just(Unit)
        }

        return Single.error(WebSocketException.UnableToSendData())
    }

    /**
     * Send data-based message to server
     *
     * @params string Data-based message to send
     *
     *  @returns A single wrapping `Void` fired when send request completes
     */
    public fun sendData(data: ByteArray): Single<Unit> {
        val bytes = ByteString.of(data, 0, data.size)

        if (socket?.send(bytes) == true) {
            return Single.just(Unit)
        }

        return Single.error(WebSocketException.UnableToSendData())
    }

    // WebSocketListener method overrides

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onDisconnect()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onDisconnect(t)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val data = bytes.toByteArray()
        incomingSubject.onNext(WebIncomingData(data))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        incomingSubject.onNext(WebIncomingText(text))
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        var isManualClose = false

        accessQueue.withLock {
            isManualClose = this.isManualClose
            isConnected = true
            reconnectAttempts = 0
        }

        connectionStateSubject.onNext(WebConnectionConnected())

        if (isManualClose) {
            disconnectSocket()
        }
    }

    private fun onDisconnect(t: Throwable? = null) {
        var isManualClose = false
        var delay: Long = 0

        accessQueue.withLock {
            isManualClose = this.isManualClose
            reconnectAttempts += 1

            val min = minReconnectDelay * reconnectAttempts
            delay = if (min > maxReconnectDelay) maxReconnectDelay else min
            isConnected = false
        }

        connectionStateSubject.onNext(WebConnectionDisconnected(t))

        // check if the connection was manually re-established. If so, make sure we reconnect.
        if (!isManualClose) {
            // FIXME: hish - check internet connection before proceeding

            Single.timer(delay, TimeUnit.SECONDS)
                .map {
                    accessQueue.withLock {
                        if (this.isManualClose) {
                            disconnectSocket()
                        } else {
                            connectSocket()
                        }
                    }
                }
                .subscribe()
        }
    }

    // Socket helpers

    private fun connectSocket() {
        val request = Request.Builder().url(this.url).build()
        client.dispatcher().cancelAll()
        socket = client.newWebSocket(request, this)
    }

    private fun disconnectSocket() {
        client.dispatcher().cancelAll()
        socket = null
    }
}
