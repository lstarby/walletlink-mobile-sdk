package com.coinbase.walletlink

import android.content.Context
import com.coinbase.crypto.extensions.sha256
import com.coinbase.store.Store
import com.coinbase.walletlink.concurrency.OperationQueue
import com.coinbase.walletlink.exceptions.WalletLinkExeception
import com.coinbase.walletlink.extensions.asUnit
import com.coinbase.walletlink.extensions.encryptUsingAES256GCM
import com.coinbase.walletlink.extensions.logError
import com.coinbase.walletlink.extensions.takeSingle
import com.coinbase.walletlink.interfaces.WalletLinkInterface
import com.coinbase.walletlink.models.ClientMetadataKey
import com.coinbase.walletlink.models.Session
import com.coinbase.walletlink.utils.ByteArrayUtils
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import java.util.concurrent.TimeUnit

data class SignatureRequest(val eventId: Int)

public class WalletLink(url: String, context: Context) : WalletLinkInterface {
    private val linkStore = LinkStore(Store(context))
    private var connectionDisposable: Disposable? = null
    private val connection = WalletLinkConnection(url)
    private val operationQueue = OperationQueue(1)
    private val isConnectedSubject = ReplaySubject.create<Boolean>(1)
    private val signatureRequestsSubject = PublishSubject.create<SignatureRequest>()
    private var metadata = mutableMapOf<ClientMetadataKey, String>()

    // Incoming signature requests
    override val signatureRequestsObservable: Observable<SignatureRequest> = signatureRequestsSubject.hide()

    /**
     * Starts WalletLink connection with the server if a stored session exists. Otherwise, this is a noop. This method
     * should be called immediately on app launch.
     *
     * @param metadata client metadata forwarded to host once link is established
     */
    override fun start(metadata: Map<ClientMetadataKey, String>) {
        this.metadata = metadata.toMutableMap()

        connectionDisposable?.dispose()

        connectionDisposable = linkStore.observeSessions()
            .flatMap { sessionIds ->
                val connSingle = if (sessionIds.isNotEmpty()) {
                    // If credentials list is not empty, try connecting to WalletLink server
                    startConnection()
                } else {
                    // Otherwise, disconnect
                    startConnection()
                }

                connSingle.onErrorResumeNext { Single.just(Unit) }.toObservable()
            }
            .subscribe()
    }

    // Disconnect from WalletLink server and stop observing session ID updates to prevent reconnection.
    override fun stop() {
        // FIXME: hish operationQueue.cancelAllOperations()
        connectionDisposable?.dispose()
        connectionDisposable = null
        stopConnection().subscribe()
    }

    /**
     * Connect to WalletLink server using parameters extracted from QR code scan
     *
     * @param sessionId WalletLink host generated session ID
     * @param secret WalletLink host/guest shared secret
     *
     * @return A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
     */
    override fun connect(sessionId: String, secret: String): Single<Unit> {
        val session = Session(sessionId, secret)

        // Connect to WalletLink server (if disconnected)
        startConnection().subscribe()

        // wait for connection to be established, then attempt to join and persist the new session.
        return isConnectedSubject
            .filter { it }
            .takeSingle()
            .flatMap { joinSession(session) }
            .map { _ -> linkStore.save(session.sessionId, session.secret) }
            .timeout(15, TimeUnit.SECONDS)
            .logError()
    }

    /**
     * Set metadata in all active sessions. This metadata will be forwarded to all the hosts
     *
     * @param key Metadata key
     * @param value Metadata value
     *
     * @return True if the operation succeeds
     */
    override fun setMetadata(key: ClientMetadataKey, value: String): Single<Unit> {
        this.metadata[key] = value

        val setMetadataSingles = linkStore.sessions.mapNotNull { session ->
            val iv = ByteArrayUtils.randomBytes(12)

            try {
                val encryptedValue = value.encryptUsingAES256GCM(session.secret, iv)
                return@mapNotNull connection.setMetadata(key, encryptedValue, session.sessionId)
                    .logError()
                    .onErrorResumeNext { Single.just(false) }
            } catch (err: WalletLinkExeception.unableToEncryptData) {
                return@mapNotNull null
            }
        }

        return Single.zip(setMetadataSingles) { it.filterIsInstance<Unit>() }.asUnit()
    }

    /**
     * Send signature request approval to the requesting host
     *
     * @param requestId WalletLink request ID
     * @param signedData User signed data
     *
     * @return A single wrapping a `Void` if successful, or an exception is thrown
     */
    override fun approve(requestId: String, signedData: ByteArray): Single<Unit> {
        return Single.just(Unit)
    }

    /**
     * Send signature request rejection to the requesting host
     *
     * @param requestId WalletLink request ID
     *
     * @return A single wrapping a `Void` if successful, or an exception is thrown
     */
    override fun reject(requestId: String): Single<Unit> {
        return Single.just(Unit)
    }

    // Connection management

    private fun startConnection(): Single<Unit> {
// FIXME: hish       operationQueue.cancelAllOperations()
//        let connectSingle = Internet.statusChanges
//                .filter { $0.isOnline }

        val connectSingle = connection.connect()
            .map { isConnectedSubject.onNext(true) }
            .flatMap { joinSessions() }

        return operationQueue.addSingle(connectSingle)
    }

    private fun stopConnection(): Single<Unit> {
//       FIXME: hish operationQueue.cancelAllOperations()

        val disconnectSingle = connection.disconnect()
            .logError()
            .onErrorResumeNext { Single.just(Unit) }
            .map { isConnectedSubject.onNext(false) }

        return operationQueue.addSingle(disconnectSingle)
    }

    // MARK: - Session management

    private fun joinSessions(): Single<Unit> {
        val joinSessionSingles = linkStore.sessions
            .map { joinSession(it).asUnit().onErrorResumeNext { Single.just(Unit) } }

        return Single.zip(joinSessionSingles) { }.asUnit()
    }

    private fun joinSession(session: Session): Single<Boolean> {
        val sessionKey = "${session.sessionId} ${session.secret} WalletLink".sha256()

        return connection.joinSession(sessionKey, session.sessionId)
            .flatMap { success ->
                if (!success) {
                    return@flatMap Single.just(false)
                }

                setSessionConfig(session)
            }
            .map { success ->
                if (success) {
                    println("[walletlink] successfully join session ${session.sessionId}")
                } else {
                    print("[walletlink] Invalid session ${session.sessionId}. Removing...")
                    linkStore.delete(session.sessionId)
                }

                success
            }
            .logError()
    }

    private fun setSessionConfig(session: Session): Single<Boolean> {
        val iv = ByteArrayUtils.randomBytes(12)
        val encryptedMetadata = mutableMapOf<String, String>()

        for ((key, value) in metadata) {
            try {
                val encryptedValue = value.encryptUsingAES256GCM(session.secret, iv)
                encryptedMetadata[key.rawValue] = encryptedValue
            } catch (err: WalletLinkExeception.unableToEncryptData) {
                return Single.error(err)
            }
        }

        return connection.setSessionConfig(
            webhookId = "1", // FIXME: hish - fill
            webhookUrl = "http://www.walletlink.org", // FIXME: hish - fill
            metadata = encryptedMetadata,
            sessionId = session.sessionId
        )
    }
}