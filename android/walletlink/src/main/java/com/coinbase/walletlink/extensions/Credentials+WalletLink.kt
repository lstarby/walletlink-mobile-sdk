package com.coinbase.walletlink.extensions

import com.coinbase.wallet.crypto.extensions.sha256
import com.coinbase.wallet.http.models.Credentials

fun Credentials.Companion.create(sessionId: String, secret: String): Credentials {
    val password = "$sessionId, $secret WalletLink".sha256()
    return Credentials(username = sessionId, password = password)
}
