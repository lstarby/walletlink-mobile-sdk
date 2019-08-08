package com.coinbase.wallet.http

import java.lang.Exception

sealed class HTTPException(msg: String) : Exception(msg) {
    object UnabelToDeserialize : HTTPException("Unable to deserialize response")
}
