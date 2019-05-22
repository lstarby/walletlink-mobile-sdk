package com.coinbase.walletlink.interfaces

import com.coinbase.walletlink.SignatureRequest
import com.coinbase.walletlink.models.ClientMetadataKey
import io.reactivex.Observable
import io.reactivex.Single

interface WalletLinkInterface {
    // Incoming signature requests
    val signatureRequestsObservable: Observable<SignatureRequest>

    /**
    * Starts WalletLink connection with the server if a stored session exists. Otherwise, this is a noop. This method
    * should be called immediately on app launch.
    *
    * @param metadata client metadata forwarded to host once link is established
    */
    fun connect(metadata: Map<ClientMetadataKey, String>)

    // Disconnect from WalletLink server and stop observing session ID updates to prevent reconnection.
    fun disconnect()

    /**
     * Connect to WalletLink server using parameters extracted from QR code scan
     *
     * @param sessionId WalletLink host generated session ID
     * @param secret WalletLink host/guest shared secret
     *
     * @return A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
     */
    fun link(sessionId: String, secret: String): Single<Unit>

    /**
     * Set metadata in all active sessions. This metadata will be forwarded to all the hosts
     *
     * @param key Metadata key
     * @param value Metadata value
     *
     * @return True if the operation succeeds
     */
    fun setMetadata(key: ClientMetadataKey, value: String): Single<Unit>

    /**
     * Send signature request approval to the requesting host
     *
     * @param requestId WalletLink request ID
     * @param signedData: User signed data
     *
     * @return A single wrapping a `Void` if successful, or an exception is thrown
     */
    fun approve(requestId: String, signedData: ByteArray): Single<Unit>

    /**
     * Send signature request rejection to the requesting host
     *
     * @param requestId WalletLink request ID
     *
     * @return A single wrapping a `Void` if successful, or an exception is thrown
     */
    fun reject(requestId: String): Single<Unit>
}