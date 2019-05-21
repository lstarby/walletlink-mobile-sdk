package com.coinbase.walletlink

import com.coinbase.walletlink.concurrency.OperationQueue
import com.coinbase.walletlink.interfaces.WalletLinkInterface
import com.coinbase.walletlink.models.ClientMetadataKey
import com.coinbase.walletlink.models.Session
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

data class SignatureRequest(val eventId: Int)

public class WalletLink(val url: String) : WalletLinkInterface {
    private val connection = WalletLinkConnection(url)
    private val operationQueue = OperationQueue(1)
    private val signatureRequestsSubject = PublishSubject.create<SignatureRequest>()

    // Incoming signature requests
    override val signatureRequestsObservable: Observable<SignatureRequest> = signatureRequestsSubject.hide()

    /**
     * Starts WalletLink connection with the server if a stored session exists. Otherwise, this is a noop. This method
     * should be called immediately on app launch.
     *
     * @param metadata client metadata forwarded to host once link is established
     */
    override fun start(metadata: Map<ClientMetadataKey, String>) {
    }

    // / Disconnect from WalletLink server and stop observing session ID updates to prevent reconnection.
    override fun stop() {
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

        return Single.just(Unit)
    }

    /**
     * Set metadata in all active sessions. This metadata will be forwarded to all the hosts
     *
     * @param key Metadata key
     * @param value Metadata value
     *
     * @return True if the operation succeeds
     */
    override fun setMetadata(key: String, value: String): Single<Unit> {
        return Single.just(Unit)
    }
    /**
     * Send signature request approval to the requesting host
     *
     * @param requestId WalletLink request ID
     * @param signedData: User signed data
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
//        operationQueue.cancelAllOperations()

//        let connectSingle = Internet.statusChanges
//                .filter { $0.isOnline }
//            .takeSingle()
//            .flatMap { _ in self.connection.connect() }
//            .map { self.isConnectedSubject.onNext(true) }
//
//        return operationQueue.addSingle(connectSingle)
        return Single.just(Unit)
    }

    private fun stopConnection(): Single<Unit> {
        return Single.just(Unit)
//        operationQueue.cancelAllOperations()
//
//        let disconnectSingle = connection.disconnect()
//            // .logError()
//            .catchErrorJustReturn(())
//            .map { self.isConnectedSubject.onNext(false) }
//
//        return operationQueue.addSingle(disconnectSingle)
    }
}