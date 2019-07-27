package com.coinbase.walletlink

import android.content.Context
import com.coinbase.wallet.core.extensions.asUnit
import com.coinbase.wallet.core.extensions.reduceIntoMap
import com.coinbase.wallet.store.Store
import com.coinbase.wallet.store.models.Optional
import com.coinbase.walletlink.exceptions.WalletLinkException
import com.coinbase.walletlink.interfaces.WalletLinkInterface
import com.coinbase.walletlink.models.HostRequest
import com.coinbase.walletlink.models.HostRequestId
import com.coinbase.walletlink.models.Session
import com.coinbase.walletlink.models.ClientMetadataKey
import com.coinbase.walletlink.storage.SessionStore
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * WalletLink SDK interface
 *
 * @property userId User ID to deliver push notifications to
 * @property notificationUrl Webhook URL used to push notifications to mobile client
 */
class WalletLink(private val userId: String, private val notificationUrl: URL, context: Context) : WalletLinkInterface {
    private val disposeBag = CompositeDisposable()
    private val requestsSubject = PublishSubject.create<HostRequest>()
    private val sessionStore = SessionStore(Store(context))
    private val requestsScheduler = Schedulers.single()
    private var connections = ConcurrentHashMap<URL, WalletLinkConnection>()

    override val requestsObservable: Observable<HostRequest> = requestsSubject.hide()

    override fun connect(metadata: ConcurrentHashMap<ClientMetadataKey, String>) {
        val connections = ConcurrentHashMap<URL, WalletLinkConnection>()
        val sessionsByUrl = sessionStore.sessions.reduceIntoMap(HashMap<URL, List<Session>>()) { acc, session ->
            val sessions = acc[session.rpcUrl]?.toMutableList()?.apply { add(session) }

            acc[session.rpcUrl] = sessions?.toList() ?: mutableListOf(session)
        }

        for ((rpcUrl, sessions) in sessionsByUrl) {
            val conn = WalletLinkConnection(
                url = rpcUrl,
                userId = userId,
                notificationUrl = notificationUrl,
                sessionStore = sessionStore,
                metadata = metadata
            )

            observeConnection(conn)
            sessions.forEach { connections[it.rpcUrl] = conn }
        }

        this.connections = connections
    }

    override fun disconnect() {
        disposeBag.clear()
        connections.values.forEach { it.disconnect() }
        connections.clear()
    }

    override fun link(
        sessionId: String,
        name: String,
        secret: String,
        rpcUrl: URL,
        metadata: ConcurrentHashMap<ClientMetadataKey, String>
    ): Single<Unit> {
        connections[rpcUrl]?.let { connection ->
            return connection.link(sessionId = sessionId, name = name, secret = secret)
        }

        val connection = WalletLinkConnection(
            url = rpcUrl,
            userId = userId,
            notificationUrl = notificationUrl,
            sessionStore = sessionStore,
            metadata = metadata
        )

        connections[rpcUrl] = connection

        return connection.link(sessionId = sessionId, name = name, secret = secret)
            .map { observeConnection(connection) }
            .onErrorResumeNext { throwable ->
                connections.remove(rpcUrl)
                throw throwable
            }
    }

    override fun setMetadata(key: ClientMetadataKey, value: String): Single<Unit> {
        val setMetadataSingles = connections.values
            .map { it.setMetadata(key = key, value = value).asUnit().onErrorReturn { Single.just(Unit) } }

        return Single.zip(setMetadataSingles) { it.filterIsInstance<Unit>() }.asUnit()
    }

    override fun approve(requestId: HostRequestId, signedData: ByteArray): Single<Unit> {
        val connection = connections[requestId.rpcUrl] ?: return Single.error(
            WalletLinkException.NoConnectionFound(requestId.rpcUrl)
        )

        return connection.approve(
            sessionId = requestId.sessionId,
            requestId = requestId.id,
            signedData = signedData
        )
    }

    override fun reject(requestId: HostRequestId): Single<Unit> {
        val connection = connections[requestId.rpcUrl] ?: return Single.error(
            WalletLinkException.NoConnectionFound(requestId.rpcUrl)
        )

        return connection.reject(sessionId = requestId.sessionId, requestId = requestId.id)
    }

    override fun getRequest(eventId: String, sessionId: String, rpcUrl: URL): Single<HostRequest> {
        val connection = connections[rpcUrl] ?: return Single.error(WalletLinkException.NoConnectionFound(rpcUrl))

        return connection.getRequest(eventId = eventId, sessionId = sessionId)
    }

    // MARK: - Helpers

    private fun observeConnection(conn: WalletLinkConnection) {
        conn.requestsObservable
            .map { println("hish $it"); it }
            .observeOn(requestsScheduler)
            .map { Optional(it) }
            .onErrorReturn { Optional(null) }
            .subscribe { request -> request.element?.let { requestsSubject.onNext(it) } }
            .let { disposeBag.add(it) }
    }
}
