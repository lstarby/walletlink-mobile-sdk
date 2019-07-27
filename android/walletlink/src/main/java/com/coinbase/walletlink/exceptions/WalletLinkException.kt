package com.coinbase.walletlink.exceptions

import java.net.URL

sealed class WalletLinkException(msg: String) : Exception(msg) {
    /**
     * Unable to encrypt message to send to host
     */
    object UnableToEncryptData : WalletLinkException("Unable to encrypt data")

    /**
     * Unable to decrypt message from host
     */
    object UnableToDecryptData : WalletLinkException("Unable to decrypt data")

    /**
     * Thrown when trying to conenct with an invalid session
     */
    object InvalidSession : WalletLinkException("Unable to encrypt data")

    /**
     * Thrown if unable to find connection for given sessionId
     */
    class NoConnectionFound(val url: URL) : WalletLinkException("Unable to find for url $url")
}
