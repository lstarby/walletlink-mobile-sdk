package com.coinbase.walletlink.exceptions

class WebSocketException {
    object UnableToSendData : RuntimeException("Unable to send data/text on live websocket")
}
