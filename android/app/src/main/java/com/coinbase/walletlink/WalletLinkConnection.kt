package com.coinbase.walletlink

import android.util.LruCache
import com.coinbase.networking.models.WebIncomingDataType
import com.coinbase.networking.models.WebIncomingText
import com.coinbase.networking.websocket.WebSocket
import com.coinbase.walletlink.extensions.jsonMap
import com.coinbase.walletlink.extensions.logError
import com.coinbase.walletlink.extensions.takeSingle
import com.coinbase.walletlink.interfaces.JsonSerializable
import com.coinbase.walletlink.models.ClientMetadataKey
import com.coinbase.walletlink.models.JoinSessionMessage
import com.coinbase.walletlink.models.MessageResponse
import com.coinbase.walletlink.models.PublishEventMessage
import com.coinbase.walletlink.models.ServerMessageType
import com.coinbase.walletlink.models.SetMetadataMessage
import com.coinbase.walletlink.models.SetSessionConfigMessage
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MessageEvent // FIXME: hish

private data class WalletLinkCallback(val requestId: Int, val subject: ReplaySubject<MessageResponse>)

private const val sendTimeout: Long = 15

class WalletLinkConnection(url: String) {
    private val disposeBag = CompositeDisposable()
    private val connection = WebSocket(url)
    private val requestIdSequence = AtomicInteger()
    private val incomingHostEventsSubject = PublishSubject.create<MessageEvent>()
    private var pendingSignerRequests = LruCache<Int, ReplaySubject<MessageResponse>>(300)
    private var pendingSignerRequestsAccessQueue = ReentrantLock()

    // Incoming WalletLink host requests
    val incomingHostEventsObservable = incomingHostEventsSubject.hide()

    // WalletLink Connection state
    val connectionStateObservable = connection.connectionStateObservable

    init {
        connection.incomingObservable
            .observeOn(Schedulers.io())
            .subscribe { handleIncomingType(it) }
            .let { disposeBag.add(it) } // FIXME: hish - is this correct?
    }

    // Connect to WalletLink server
    fun connect(): Single<Unit> {
        return connection.connect()
    }

    // Disconnect from WalletLink server
    fun disconnect(): Single<Unit> {
        return connection.disconnect()
    }

    /**
     * Join a WalletLink session
     *
     * @param sessionKey sha256(session+secret) hash
     * @param sessionId:session ID scanned offline (QR code, NFC, etc)
     *
     * @return True if session is valid or false if invalid. An exception will be thrown if a network or connection
     *         error occurs while in flight.
     */
    fun joinSession(sessionKey: String, sessionId: String): Single<Boolean> {
        val callback = createCallback()
        val message = JoinSessionMessage(
            requestId = callback.requestId,
            sessionId = sessionId,
            sessionKey = sessionKey
        )

        return send(message, callback)
    }

    /**
     * Set metadata in the current session
     *
     * @param key Metadata key on WalletLink server
     * @param value Metadata value stored on WalletLink server. This data may be encrypted.
     * @param sessionId Session ID scanned offline (QR code, NFC, etc)
     *
     * @return True if the operation succeeds
     */
    fun setMetadata(key: ClientMetadataKey, value: String, sessionId: String): Single<Boolean> {
        val callback = createCallback()
        val message = SetMetadataMessage(
            requestId = callback.requestId,
            sessionId = sessionId,
            key = key.rawValue,
            value = value
        )

        return send(message, callback)
    }

    /**
     * Set session config once a link is established
     *
     * @param webhookId FIXME: hish
     * @param webhookUrl FIXME: hish
     * @param metadata Metadata forwarded to host
     * @param sessionId Session ID scanned offline (QR code, NFC, etc)
     *
     * @return True if the operation succeeds
     */
    fun setSessionConfig(
        webhookId: String,
        webhookUrl: String,
        metadata: Map<String, String>,
        sessionId: String
    ): Single<Boolean> {
        val callback = createCallback()
        val message = SetSessionConfigMessage(
            requestId = callback.requestId,
            sessionId = sessionId,
            webhookId = webhookId,
            webhookUrl = webhookUrl,
            metadata = metadata
        )

        return send(message, callback)
    }

    /**
     * Publish new event to WalletLink server
     *
     * @param event Event name to publish
     * @param data the data associated with this event
     * @param sessionId Session ID scanned offline (QR code, NFC, etc)
     *
     * @return Trie if the operation succeeds
     */
    fun publishEvent(event: String, data: Map<String, String>, sessionId: String): Single<Boolean> {
        val callback = createCallback()
        val message = PublishEventMessage(
            requestId = callback.requestId,
            sessionId = sessionId,
            event = event,
            data = data
        )

        return send(message, callback)
    }

    // Send message helper(s)

    private fun send(message: JsonSerializable, callback: WalletLinkCallback): Single<Boolean> {
        val jsonString = message.asJsonString()

        return connection.sendString(jsonString)
            .flatMap { callback.subject.takeSingle() }
            .map { it.type == MessageResponse.ResponseType.OK }
            .timeout(sendTimeout, TimeUnit.SECONDS)
            .doOnError { deleteCallback(callback.requestId) }
            .logError()
    }

    // Pending requests callback management

    private fun createCallback(): WalletLinkCallback {
        val requestId = requestIdSequence.incrementAndGet()
        val subject = ReplaySubject.create<MessageResponse>(1)

        pendingSignerRequestsAccessQueue.withLock { pendingSignerRequests.put(requestId, subject) }

        return WalletLinkCallback(requestId = requestId, subject = subject)
    }

    private fun deleteCallback(requestId: Int) {
        pendingSignerRequestsAccessQueue.withLock { pendingSignerRequests.remove(requestId) }
    }

    // / This is called when the host sends a response to a request initiated by the signer
    private fun receivedMessageResponse(response: MessageResponse) {
        val requestId = response.id
        if (requestId == null) {
            assert(false) // FIXME: hish , "Invalid WalletLink message link response")
            return
        }

        var subject: ReplaySubject<MessageResponse>? = null

        pendingSignerRequestsAccessQueue.withLock {
            subject = pendingSignerRequests.get(requestId)
            pendingSignerRequests.remove(response.id)
        }

        subject?.onNext(response)
    }

    // this is called when the host sends a request initiated by the host
    private fun receivedMessageEvent(event: MessageEvent) {
        incomingHostEventsSubject.onNext(event)
    }

    private fun handleIncomingType(incoming: WebIncomingDataType) {
        val incomingText = incoming as? WebIncomingText ?: return
        val jsonString = incomingText.string
        val json = jsonString.jsonMap() ?: return
        val typeString = json["type"] as? String ?: return
        val type = ServerMessageType.fromRawValue(typeString) ?: return

        when (type) {
            ServerMessageType.OK, ServerMessageType.FAIL -> {
                val response = MessageResponse.fromJsonString(jsonString) ?: return
                receivedMessageResponse(response)
            }
            ServerMessageType.EVENT -> {
                TODO("Missing event implementation. Call receivedMessageEvent") // FIXME: hish
            }
        }
    }
}
